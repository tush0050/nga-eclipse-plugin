/**
 * 
 */
package org.hp.qc.web.restapi.docexamples.docexamples;

import org.hp.qc.web.restapi.docexamples.docexamples.infrastructure.Assert;
import org.hp.qc.web.restapi.docexamples.docexamples.infrastructure.*;
import org.hp.qc.web.restapi.docexamples.docexamples.infrastructure.Entity.Fields.Field;

//import javax.management.monitor.StringMonitor;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nathan
 * 
 * this example shows how to change data on already existing entities.
 * 
 */

public class UpdateExample {

    public static void main(String[] args) throws Exception {
        new UpdateExample().updateExample(
                "http://" + Constants.HOST + ":" +
                        Constants.PORT + "/qcbin",
                Constants.DOMAIN,
                Constants.PROJECT,
                Constants.USERNAME,
                Constants.PASSWORD);
    }

    public void updateExample(final String serverUrl, final String domain, final String project, String username, String password) throws Exception {

        RestConnector con =
                RestConnector.getInstance().init(
                        new HashMap<String, String>(),
                        serverUrl,
                        domain,
                        project);
        
        AuthenticateLoginLogoutExample login = new AuthenticateLoginLogoutExample();
        CreateDeleteExample writeExample = new CreateDeleteExample();
        UpdateExample example = new UpdateExample();
        
        //we use the example code of how to login to handle our login in this example.
        boolean loginResponse = login.login(username, password);
        Assert.assertTrue("login failed", loginResponse);
        
        String exampleEntityType = "requirement";
        String requirementsUrl = con.buildEntityCollectionUrl(exampleEntityType);
        
        //we use the example code of creating an entity to make an entity for us to update.
        String newEntityToUpdateUrl =
                writeExample.createEntity(requirementsUrl, Constants.entityToPostXml);
        
        //create xml that when posted modifies the entity
        String updatedField = "request-note";
        String updatedFieldInitialUpdateValue = "im an updated value";
        
        String updatedEntityXml =
                generateSingleFieldUpdateXml(updatedField, updatedFieldInitialUpdateValue);
        
        //checkout (or lock) the entity - depending on versioning support.
        boolean isVersioned = Constants.isVersioned(exampleEntityType, domain, project);
        String preModificationXml = null;
        if (isVersioned) {
            
            //note that we selected an entity that supports versioning on a project that supports versioning. would fail otherwise.
            String firstCheckoutComment = "check out comment1";
            preModificationXml = example.checkout(newEntityToUpdateUrl, firstCheckoutComment, -1);
            Assert.assertTrue(
                    "checkout comment missing",
                    preModificationXml.contains(Constants.generateFieldXml(
                            "vc-checkout-comments",
                            firstCheckoutComment)));
        }

        else {
            
            preModificationXml = example.lock(newEntityToUpdateUrl);
        }
        
        Assert.assertTrue(
                "posted field value not found",
                preModificationXml.contains(Constants.entityToPostFieldXml));
        
        //update the entity
        String put = example.update(newEntityToUpdateUrl, updatedEntityXml).toString();
        Assert.assertTrue("posted field value not found", put.contains(Constants.generateFieldXml(
                updatedField,
                updatedFieldInitialUpdateValue)));
        
        //checkin (or unlock) the entity - depending on versioning support.
        if (isVersioned) {

            String firstCheckinComment = "check in comment1";
            boolean checkin = example.checkin(newEntityToUpdateUrl, firstCheckinComment, false);
            Assert.assertTrue("checkin failed", checkin);
        }

        else {
            
            boolean unlock = example.unlock(newEntityToUpdateUrl);
            Assert.assertTrue("unlock failed", unlock);
        }
        
        /*
          
         now we do the same thing again, only this time with marshalling
          
         */

        //checkout
        if (isVersioned) {
            
            preModificationXml = example.checkout(newEntityToUpdateUrl, "check out comment2", -1);
        }

        else {
            
            preModificationXml = example.lock(newEntityToUpdateUrl);
        }
        
        Assert.assertTrue(
                "posted field value not found",
                preModificationXml.contains(Constants.generateFieldXml(
                        updatedField,
                        updatedFieldInitialUpdateValue)));
        
        //create update string
        String updatedFieldUpdatedValue = "updating via marshal / unmarhsalling";
        String entityUpdateXml =
                generateSingleFieldUpdateXml(updatedField, updatedFieldUpdatedValue);
        //create entity (we could have instantiated the entity and used methods to set the new values
        Entity e = EntityMarshallingUtils.marshal(Entity.class, entityUpdateXml);
        
        //do update operation
        String updateResponseEntityXml =
                example.update(
                        newEntityToUpdateUrl,
                        EntityMarshallingUtils.unmarshal(Entity.class, e)).toString();
        
        //entity xml from server -> entity class instance
        Entity updateResponseEntity =
                EntityMarshallingUtils.marshal(Entity.class, updateResponseEntityXml);
        
        boolean updatedValueEncountered = false;
        List<Field> fields = updateResponseEntity.getFields().getField();
        for (Field field : fields) {
            if (field.getName().equals(updatedField)) {
                Assert.assertEquals(
                        "updated value different than expected",
                        field.getValue().iterator().next(),
                        updatedFieldUpdatedValue);
                updatedValueEncountered = true;
                break;
            }
        }
        Assert.assertTrue("did not encounter updated value", updatedValueEncountered);
        
        //checkin
        if (isVersioned) {
            boolean checkin = example.checkin(newEntityToUpdateUrl, null, false);
            Assert.assertTrue("checkin failed", checkin);
        }

        else {
            
            boolean unlock = example.unlock(newEntityToUpdateUrl);
            Assert.assertTrue("unlock failed", unlock);
        }
        
        //cleanup
        writeExample.deleteEntity(newEntityToUpdateUrl);
        login.logout();
        
    }
    
    private RestConnector con;
    
    /**
     * @param
     */
    public UpdateExample() {
        con = RestConnector.getInstance();
    }
    
    /**
     * @param entityUrl
     *            of the entity to checkout
     * @param comment
     *            to keep on the server side of why you checked this entity out
     * @param version
     *            to checkout or -1 if you want the latest
     * @return a string description of the checked out entity
     * @throws Exception
     */
    public String checkout(String entityUrl, String comment, int version) throws Exception {
        
        String commentXmlBit =
                ((comment != null) && !comment.isEmpty()
                        ? "<Comment>" + comment + "</Comment>"
                        : "");
        
        String versionXmlBit = (version >= 0 ? "<Version>" + version + "</Version>" : "");
        
        String xmlData = commentXmlBit + versionXmlBit;
        
        String xml =
                xmlData.isEmpty() ? "" : "<CheckOutParameters>" + xmlData + "</CheckOutParameters>";
        
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/xml");
        requestHeaders.put("Accept", "application/xml");
        
        Response response =
                con.httpPost(entityUrl + "/versions/check-out", xml.getBytes(), requestHeaders);
        
        if (response.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception(response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * @param entityUrl
     *            to checkin
     * @param comment
     *            this will override any comment you made in the checkout
     * @param overrideLastVersion
     *            this will override last version
     * @return true if operation is successful
     * @throws Exception
     */
    public boolean checkin(String entityUrl, String comment, boolean overrideLastVersion) throws Exception {

        final String commentXmlBit =
                ((comment != null) && !comment.isEmpty()
                        ? "<Comment>" + comment + "</Comment>"
                        : "");

        final String overrideLastVersionBit = overrideLastVersion == true ? "<OverrideLastVersion>true</OverrideLastVersion>" : "" ;

        final String xmlData = commentXmlBit + overrideLastVersionBit;

        final String xml =
                xmlData.isEmpty() ? "" : "<CheckInParameters>" + xmlData + "</CheckInParameters>";

        final Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/xml");

        //just execute a post operation on the checkin resource of your entity
        Response response = con.httpPost(entityUrl + "/versions/check-in", xml.getBytes(), requestHeaders);
        
        boolean ret = response.getStatusCode() == HttpURLConnection.HTTP_OK;
        
        return ret;
    }
    
    /**
     * @param entityUrl
     *            to lock
     * @return the locked entity xml
     * @throws Exception
     */
    public String lock(String entityUrl) throws Exception {
        
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Accept", "application/xml");
        
        Response lockResponse = con.httpPost(entityUrl + "/lock", "", requestHeaders);
        if (lockResponse.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception(lockResponse.toString());
        }
        return lockResponse.toString();
    }
    
    /**
     * @param entityUrl
     *            to unlock
     * @return
     * @throws Exception
     */
    public boolean unlock(String entityUrl) throws Exception {
        
        return con.httpDelete(entityUrl + "/lock", null).getStatusCode() == HttpURLConnection.HTTP_OK;
    }
    
    /**
     * @param field
     *            the field name to update
     * @param value
     *            the new value to use
     * @return an xml that can be used to update an entity's single given field to given value
     */
    private static String generateSingleFieldUpdateXml(String field, String value) {
        return "<Entity Type=\"requirement\"><Fields>"
               + Constants.generateFieldXml(field, value)
               + "</Fields></Entity>";
    }
    
    /**
     * @param entityUrl
     *            to update
     * @param updatedEntityXml
     *            new entity descripion. only lists updated fields. unmentioned fields will not
     *            change.
     * @return xml description of the entity on the serverside, after update.
     * @throws Exception
     */
    private Response update(String entityUrl, String updatedEntityXml) throws Exception {
        
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/xml");
        requestHeaders.put("Accept", "application/xml");
        
        Response putResponse = con.httpPut(entityUrl, updatedEntityXml.getBytes(), requestHeaders);
        
        if (putResponse.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception(putResponse.toString());
        }
        
        return putResponse;
    }
    
}
