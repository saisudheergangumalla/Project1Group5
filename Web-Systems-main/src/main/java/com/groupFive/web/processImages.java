package com.groupFive.web;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import org.apache.commons.io.IOUtils;


import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.protobuf.ByteString;

@WebServlet("/com.groupFive.web.processImages")
public class processImages extends HttpServlet {

    /**
     * This function is reposible for contacting the data base as well as the
     * cloud vision API. Finally, it sends the obtained data to the front end
     * JSP for user interface implementation.<br>
     * @param request request feed sent from home.jsp
     * @param response response feed sent from home.jsp
     * @throws ServletException Handle Servlet Exceptions
     * @throws IOException Handle IO Exceptions
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //Create dataStore instance
        DatastoreService dataStore = DatastoreServiceFactory.getDatastoreService();

        String userID = (String)request.getParameter("userID");

        ArrayList<String> photoID = new ArrayList<String>(Arrays.asList(request.getParameterValues("imageID")[0].split(",")));
        ArrayList<String> imageLinks = new ArrayList<String>(Arrays.asList(request.getParameterValues("imageLinks")[0].split(",")));

        processImage(dataStore, imageLinks, userID, photoID);


        String firstImage = "0";


        response.sendRedirect("/com.groupFive.web.app?userID="+userID+"&index=0");

    }


    /**
     * This function receives the specified parameters as arguments, and makes two consecutive calls,
     * one call to the vision API and the second to the data store.<br>
     * @param dataStore Google Data Store Service
     * @param imageLinks String image URL retrieved from Facebook
     * @param UserID Passed user's identification number
     * @param photoID Passed photo's identification number
     */
    private void processImage(DatastoreService dataStore, ArrayList<String> imageLinks, String UserID, ArrayList<String> photoID)  {



        try {

            if (imageLinks != null) {
                int index = 0;

                for (String photo : imageLinks) {

                    Entity user = ifAlreadyExists(dataStore, photoID.get(index));

                    if (user == null) {

                        //Retrieve dominant colors through Vision API
                        List<ColorInfo> dominantColors = null;
                        try {
                            dominantColors = getDominantColors(photo);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //upload image ID, image link, and dominant colors to data store
                        if (dominantColors != null) {
                            user = uploadToDataStore(dominantColors, photo, dataStore, UserID, photoID.get(index));
                        }
                    }

                    index++;

                }//end for
            }




        }//end try
        catch (Exception e) {
            e.printStackTrace();
        }
    }//end func

    //Saving to data store.
    /**
     * This function receives the data obtained from the data store and stores it in
     * the google data store. In addition, it builds a json string using the obtained
     * information and saves it to the data store. Instead of the returned ColorInfo object
     * that the cloud vision API returns.<br>
     * @param dominantColors ColorInfo Object, returned from vision API
     * @param imageLink String image URL retrieved from Facebook
     * @param datastore Google Data Store Service
     * @param userID Passed user's identification number
     * @param photoID Passed photo's identification number
     * @return Google Data Store Entity or null
     */
    private Entity uploadToDataStore(List<ColorInfo> dominantColors, String imageLink, DatastoreService datastore, String userID, String photoID) {


        if(dominantColors != null ) {

            Entity user = new Entity("User");
            String jsonArray = "{data: [";
            System.out.println(dominantColors.size());
            for (int i = 0; i < 5; i++) {
                String jsonObject = String.format("{\"red\":%f,\"green\":%f,\"blue\":%f,\"pixel\":%f,\"score\":%f}", dominantColors.get(i).getColor().getRed(),dominantColors.get(i).getColor().getGreen(),dominantColors.get(i).getColor().getBlue(),dominantColors.get(i).getPixelFraction()*100, dominantColors.get(i).getScore()*100 );
                jsonArray = jsonArray.concat(jsonObject);
                if(i+1 < 5) jsonArray = jsonArray.concat(",");
            }
            jsonArray = jsonArray.concat("]}");


            user.setProperty("user_id", userID);
            user.setProperty("fb_image_id", photoID);
            user.setProperty("image_url", imageLink);
            user.setProperty("colors", jsonArray);

            datastore.put(user);

            return user;

        }
        return null;
    }

    /**
     * This function checks if the passed photo is already stored in the Google Data Store. And
     * returns a Data Store entity accordingly.<br>
     * @param datastore Google Data Store Service
     * @param photoID Passed photo's identification number
     * @return Google Data Store Entity
     */
    private synchronized Entity ifAlreadyExists(DatastoreService datastore, String photoID) {
        Query q =
                new Query("User")
                        .setFilter(new FilterPredicate("fb_image_id", FilterOperator.EQUAL, photoID));
        PreparedQuery pq = datastore.prepare(q);
        Entity result = pq.asSingleEntity();

        return result;
    }

    /**
     * This function receives a String url and converts it to a byte [] for
     * purposes of contacting the vision API<br>
     * @param url String image URL retrieved from Facebook
     * @return byte[] array
     * @throws Exception Handle Exceptions
     */
    public static byte[] downloadFile(URL url) throws Exception {
        try (InputStream in = url.openStream()) {
            byte[] bytes = IOUtils.toByteArray(in);
            return bytes;
        }
    }


    /**
     * This function receives a String link to an image, then proceeds to call the
     * downloadFile function which returns a byte []. Then using that, calls the cloud
     * vision API requesting the IMAGE_PROPERTIES type and stores the returned result
     * in a ColorInfo object. This object is then returned to the calling function.<br>
     * @param imageLink String image URL retrieved from Facebook
     * @return ColorInfo Object, returned from vision API
     * @throws Exception Handle Exceptions
     */
    private List<ColorInfo> getDominantColors(String imageLink) throws Exception {

            byte[]  imgBytes = downloadFile(new URL(imageLink));
            ByteString byteString = ByteString.copyFrom(imgBytes);

            List<AnnotateImageRequest> requests = new ArrayList<>();

            Image img = Image.newBuilder().setContent(byteString).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);

            List<ColorInfo> dominantColors = new ArrayList<ColorInfo>();

            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests. After completing all of your requests, call
            // the "close" method on the client to safely clean up any remaining background resources.
            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        System.out.format("Error: %s%n", res.getError().getMessage());

                    }

                    // For full list of available annotations, see http://g.co/cloud/vision/docs
                    DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
                    dominantColors = colors.getColorsList();

                }

            }
            return dominantColors;


    }//end getDominantColors

}
