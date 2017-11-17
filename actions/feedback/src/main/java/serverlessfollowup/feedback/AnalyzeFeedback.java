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

/**
 * Uses Tone Analyzer to get the mood of a user feedback.
 */
public class AnalyzeFeedback {

  /**
   * Input:
   * <ul>
   *   <li>services.cloudant.url - Cloudant service url including username and password
   *   <li>services.ta.url - Tone Analyzer API endpoint</li>
   *   <li>services.ta.username - Tone Analyzer username</li>
   *   <li>services.ta.password - Tone Analyzer password</li>
   *   <li>subject - the user identifier</li>
   *   <li>message - the message to analyze</li>
   * </ul>
   * 
   * Output:
   * <ul>
   *   <li>subject - the user identifier</li>
   *   <li>message - a message to send back to the user</li>
   * </ul>
   */
  public static JsonObject main(JsonObject args) throws Exception {
    String subject = args.getAsJsonPrimitive("subject").getAsString();
    String feedbackText = args.getAsJsonPrimitive("message").getAsString();
    
    // initialize tone analyzer
    final String VERSION_DATE = "2016-05-19";
    ToneAnalyzer service = new ToneAnalyzer(VERSION_DATE);
    service.setEndPoint(args.getAsJsonPrimitive("services.ta.url").getAsString());
    service.setUsernameAndPassword(args.getAsJsonPrimitive("services.ta.username").getAsString(),
        args.getAsJsonPrimitive("services.ta.password").getAsString());
    
    // get the feedback tone
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
    System.out.println("Feedback tone is " + feedbackToneScore);
    
    if (feedbackToneScore.getScore() < 0.01) {
      System.out.println("Score is too small to send a feedback");
      throw new IllegalArgumentException("Score too small");
    }
    
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
