package serverlessfollowup.feedback;

import java.net.URL;
import java.util.Map;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.google.gson.JsonObject;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneCategory;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneScore;

public class AnalyzeFeedback {

  public static JsonObject main(JsonObject args) throws Exception {

    System.out.println(args);
    
    String subject = args.getAsJsonPrimitive("subject").getAsString();
    String feedbackText = args.getAsJsonPrimitive("message").getAsString();
    
    // get the feedback tone
    final String VERSION_DATE = "2016-05-19";
    ToneAnalyzer service = new ToneAnalyzer(VERSION_DATE);
    service.setUsernameAndPassword(args.getAsJsonPrimitive("services.ta.username").getAsString(),
        args.getAsJsonPrimitive("services.ta.password").getAsString());
    
    ToneOptions toneOptions = new ToneOptions.Builder().text(feedbackText).build();
    ToneAnalysis tone = service.tone(toneOptions).execute();

    // pick the tone with highest score
    ToneScore feedbackToneScore = null;
    for (ToneCategory category : tone.getDocumentTone().getToneCategories()) {
      if ("emotion_tone".equals(category.getCategoryId())) {
        for (ToneScore toneScore : category.getTones()) {
          if (feedbackToneScore == null || feedbackToneScore.getScore() < toneScore.getScore()) {
            feedbackToneScore = toneScore;
          }
        }
      }
    }
    System.out.println(feedbackToneScore);
    
    // look for a mood message for this tone
    CloudantClient client = ClientBuilder.url(new URL(args.getAsJsonPrimitive("services.cloudant.url").getAsString()))
        .build();
    Database moods = client.database("moods", true);

    // find the mood message for this tone
    Map<String, Object> moodMessage = moods.find(Map.class, feedbackToneScore.getToneId());
    String moodMessageTemplate = (String) moodMessage.get("template");
    System.out.println("Template for feedback is " + moodMessageTemplate);

    JsonObject response = new JsonObject();
    response.addProperty("subject", subject);
    response.addProperty("message", moodMessageTemplate);
    return response;
  }
}
