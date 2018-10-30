package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.util.files.FileAndPathUtil;

public class GNPSUtils {
  // Logger.
  private static final Logger LOG = Logger.getLogger(GNPSUtils.class.getName());

  /**
   * Submit job to GNPS
   * 
   * @param file
   * @param param
   * @return
   */
  public static String submitJob(File file, GNPSSubmitParameters param) {
    try {
      // optional
      boolean useMeta = param.getParameter(GNPSSubmitParameters.META_FILE).getValue();
      boolean ann = param.getParameter(GNPSSubmitParameters.ANN_EDGES).getValue();
      boolean corr = param.getParameter(GNPSSubmitParameters.CORR_EDGES).getValue();
      boolean openWebsite = param.getParameter(GNPSSubmitParameters.OPEN_WEBSITE).getValue();
      String presets = param.getParameter(GNPSSubmitParameters.PRESETS).getValue().toString();
      String email = param.getParameter(GNPSSubmitParameters.EMAIL).getValue();
      //
      File folder = file.getParentFile();
      String name = file.getName();
      // all file paths
      File mgf = FileAndPathUtil.getRealFilePath(folder, name, "mgf");
      File quan = FileAndPathUtil.getRealFilePath(folder, name + "_quant", "csv");

      // NEEDED files
      if (mgf.exists() && quan.exists()) {
        File edgeAnn = FileAndPathUtil.getRealFilePath(folder, name + "_edges_msannotation", "csv");
        File edgeCorr =
            FileAndPathUtil.getRealFilePath(folder, name + "_edges_ms1correlation", "csv");

        File meta = null;
        if (useMeta)
          meta =
              param.getParameter(GNPSSubmitParameters.META_FILE).getEmbeddedParameter().getValue();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
          MultipartEntity entity = new MultipartEntity();

          // ######################################################
          // NEEDED
          // tool, presets, quant table, mgf
          entity.addPart("featuretool", new StringBody("MZMINE2"));
          entity.addPart("networkingpreset", new StringBody(presets));
          entity.addPart("featurequantification", new FileBody(quan));
          entity.addPart("featurems2", new FileBody(mgf));

          // ######################################################
          // OPTIONAL
          // email, meta data, additional edges
          entity.addPart("email", new StringBody(email));
          if (useMeta && meta != null && meta.exists())
            entity.addPart("samplemetadata", new FileBody(meta));
          if (corr && edgeCorr.exists())
            entity.addPart("additionalpairs", new FileBody(edgeCorr));
          if (ann && edgeAnn.exists())
            entity.addPart("additionalpairs", new FileBody(edgeAnn));

          HttpPost httppost =
              new HttpPost("http://mingwangbeta.ucsd.edu:5050/uploadanalyzefeaturenetworking");
          httppost.setEntity(entity);

          LOG.info("Submitting GNPS job " + httppost.getRequestLine());
          CloseableHttpResponse response = httpclient.execute(httppost);
          try {
            LOG.info("GNPS submit response status: " + response.getStatusLine());
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
              LOG.info("GNPS submit response content length: " + resEntity.getContentLength());

              // open job website
              if (openWebsite)
                openWebsite(resEntity);
              EntityUtils.consume(resEntity);
            }
          } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error while submitting GNPS job", e);
            throw new MSDKRuntimeException(e);
          } finally {
            response.close();
          }
        } finally {
          httpclient.close();
        }
      }
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Error while submitting GNPS job", e);
      throw new MSDKRuntimeException(e);
    }
    return "";
  }

  /**
   * Open website with GNPS job
   * 
   * @param resEntity
   */
  private static void openWebsite(HttpEntity resEntity) {
    if (Desktop.isDesktopSupported()) {
      try {
        JSONObject res = new JSONObject(EntityUtils.toString(resEntity));
        String url = res.getString("url");
        LOG.info("Response: " + res.toString());

        if (url != null && !url.isEmpty())
          Desktop.getDesktop().browse(new URI(url));

      } catch (ParseException | IOException | URISyntaxException | JSONException e) {
        LOG.log(Level.SEVERE, "Error while submitting GNPS job", e);
      }
    }
  }
}
