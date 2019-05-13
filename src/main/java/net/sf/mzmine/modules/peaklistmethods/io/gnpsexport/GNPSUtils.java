/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

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

/**
 * Class to submit GNPS feature based molecular networking jobs directly
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GNPSUtils {
  // Logger.
  private static final Logger LOG = Logger.getLogger(GNPSUtils.class.getName());
  public static final String URL =
      "http://dorresteinappshub.ucsd.edu:5050/uploadanalyzefeaturenetworking";

  /**
   * Submit job to GNPS
   * 
   * @param file
   * @param param
   * @return
   */
  public static String submitJob(File file, GNPSSubmitParameters param)
      throws MSDKRuntimeException {
    // optional
    boolean useMeta = param.getParameter(GNPSSubmitParameters.META_FILE).getValue();
    boolean ann = param.getParameter(GNPSSubmitParameters.ANN_EDGES).getValue();
    // TODO
    // boolean corr = param.getParameter(GNPSSubmitParameters.CORR_EDGES).getValue();
    boolean corr = false;
    boolean openWebsite = param.getParameter(GNPSSubmitParameters.OPEN_WEBSITE).getValue();
    String presets = param.getParameter(GNPSSubmitParameters.PRESETS).getValue().toString();
    String title = param.getParameter(GNPSSubmitParameters.JOB_TITLE).getValue();
    String email = param.getParameter(GNPSSubmitParameters.EMAIL).getValue();
    String username = param.getParameter(GNPSSubmitParameters.USER).getValue();
    String password = param.getParameter(GNPSSubmitParameters.PASSWORD).getValue();
    //
    File folder = file.getParentFile();
    String name = file.getName();
    // all file paths
    File mgf = FileAndPathUtil.getRealFilePath(folder, name, "mgf");
    File quan = FileAndPathUtil.getRealFilePath(folder, name + "_quant", "csv");

    // NEEDED files
    if (mgf.exists() && quan.exists()) {
      File edgeAnn = !ann ? null
          : FileAndPathUtil.getRealFilePath(folder, name + "_edges_msannotation", "csv");
      File edgeCorr = !corr ? null
          : FileAndPathUtil.getRealFilePath(folder, name + "_edges_ms1correlation", "csv");

      File meta = !useMeta ? null
          : param.getParameter(GNPSSubmitParameters.META_FILE).getEmbeddedParameter().getValue();

      return submitJob(mgf, quan, meta, edgeAnn, edgeCorr, title, email, presets, openWebsite,
          username, password);
    } else
      return "";
  }


  /**
   * Submit job to GNPS
   * 
   * @param file
   * @param param
   * @return
   */
  public static String submitJob(File mgf, File quan, File meta, File edgeAnn, File edgeCorr,
      String title, String email, String presets, boolean openWebsite, String username,
      String password) throws MSDKRuntimeException {
    try {
      // NEEDED files
      if (mgf.exists() && quan.exists() && !presets.isEmpty()) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
          MultipartEntity entity = new MultipartEntity();

          // ######################################################
          // NEEDED
          // tool, presets, quant table, mgf
          entity.addPart("description", new StringBody(title == null ? "" : title));
          entity.addPart("featuretool", new StringBody("MZMINE2"));
          entity.addPart("networkingpreset", new StringBody(presets));
          entity.addPart("featurequantification", new FileBody(quan));
          entity.addPart("featurems2", new FileBody(mgf));

          // user and password
          entity.addPart("username", new StringBody(username));
          entity.addPart("password", new StringBody(password));

          // ######################################################
          // OPTIONAL
          // email, meta data, additional edges
          entity.addPart("email", new StringBody(email));
          if (meta != null && meta.exists())
            entity.addPart("samplemetadata", new FileBody(meta));
          if (edgeCorr != null && edgeCorr.exists())
            entity.addPart("additionalpairs", new FileBody(edgeCorr));
          if (edgeAnn != null && edgeAnn.exists())
            entity.addPart("additionalpairs", new FileBody(edgeAnn));

          HttpPost httppost = new HttpPost(URL);
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
