package com.linkedin.restli.internal.server.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestRestLiResponseEnvelope
{
  @Test(dataProvider = "resourceMethodProvider")
  public void testBuildBlankResponseEnvelope(ResourceMethod resourceMethod)
  {
    RestLiResponseEnvelope responseEnvelope = buildBlankResponseEnvelope(resourceMethod);
    Assert.assertNotNull(responseEnvelope);
    Assert.assertEquals(responseEnvelope.getStatus(), HttpStatus.S_200_OK);
    Assert.assertNull(responseEnvelope.getException());
    Assert.assertFalse(responseEnvelope.isErrorResponse());
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);

    switch (responseType)
    {
      case SINGLE_ENTITY:
        RecordResponseEnvelope recordResponseEnvelope = (RecordResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(recordResponseEnvelope.getRecord());
        Assert.assertTrue(recordResponseEnvelope.getRecord().getClass().isAssignableFrom(EmptyRecord.class));
        break;
      case GET_COLLECTION:
        CollectionResponseEnvelope collectionResponseEnvelope = (CollectionResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(collectionResponseEnvelope.getCollectionResponse());
        Assert.assertNotNull(collectionResponseEnvelope.getCollectionResponseCustomMetadata());
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponsePaging());
        Assert.assertTrue(collectionResponseEnvelope.getCollectionResponse().isEmpty());
        Assert.assertTrue(collectionResponseEnvelope.getCollectionResponseCustomMetadata().getClass()
                              .isAssignableFrom(EmptyRecord.class));
        break;
      case CREATE_COLLECTION:
        BatchCreateResponseEnvelope batchCreateResponseEnvelope =
            (BatchCreateResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(batchCreateResponseEnvelope.getCreateResponses());
        Assert.assertTrue(batchCreateResponseEnvelope.getCreateResponses().isEmpty());
        break;
      case BATCH_ENTITIES:
        BatchResponseEnvelope batchResponseEnvelope = (BatchResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(batchResponseEnvelope.getBatchResponseMap());
        Assert.assertTrue(batchResponseEnvelope.getBatchResponseMap().isEmpty());
        break;
      case STATUS_ONLY:
        // status only envelopes are blank by default since they have no data fields
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @DataProvider
  private Object[][] resourceMethodProvider()
  {
    ResourceMethod[] resourceMethods = ResourceMethod.values();
    Object[][] resourceMethodData = new Object[resourceMethods.length][1];
    for (int i = 0; i < resourceMethodData.length; i++)
    {
      resourceMethodData[i][0] = resourceMethods[i];
    }
    return resourceMethodData;
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testEnvelopeResourceMethodType(RestLiResponseEnvelope responseEnvelope, ResourceMethod resourceMethod)
  {
    Assert.assertEquals(responseEnvelope.getResourceMethod(), resourceMethod);
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testEnvelopeResponseType(RestLiResponseEnvelope responseEnvelope, ResourceMethod resourceMethod)
  {
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);
    Assert.assertEquals(responseEnvelope.getResponseType(), responseType);
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testSetNewEnvelopeData(RestLiResponseEnvelope responseEnvelope, ResourceMethod resourceMethod)
  {
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);

    switch (responseType)
    {
      case SINGLE_ENTITY:
        RecordResponseEnvelope recordResponseEnvelope = (RecordResponseEnvelope)responseEnvelope;
        RecordTemplate oldRecord = recordResponseEnvelope.getRecord();
        RecordTemplate newRecord = new AnyRecord(new DataMap());
        newRecord.data().put("test", "testing");
        recordResponseEnvelope.setRecord(newRecord, HttpStatus.S_200_OK);
        Assert.assertNotEquals(recordResponseEnvelope.getRecord(), oldRecord);
        break;
      case GET_COLLECTION:
        CollectionResponseEnvelope collectionResponseEnvelope = (CollectionResponseEnvelope)responseEnvelope;
        List<? extends RecordTemplate> oldResponses = collectionResponseEnvelope.getCollectionResponse();
        RecordTemplate oldResponseMetadata = collectionResponseEnvelope.getCollectionResponseCustomMetadata();
        CollectionMetadata oldPagingMetadata = collectionResponseEnvelope.getCollectionResponsePaging();

        RecordTemplate newResponseMetadata = new AnyRecord(new DataMap());
        newResponseMetadata.data().put("test", "testing");
        CollectionMetadata newResponsesPaging = new CollectionMetadata();
        List<? extends RecordTemplate> newResponses = Arrays.asList(new AnyRecord(new DataMap()));

        collectionResponseEnvelope.setCollectionResponse(newResponses,
                                                         newResponsesPaging,
                                                         newResponseMetadata,
                                                         HttpStatus.S_200_OK);

        Assert.assertNotEquals(collectionResponseEnvelope.getCollectionResponse(), oldResponses);
        Assert.assertNotEquals(collectionResponseEnvelope.getCollectionResponseCustomMetadata(), oldResponseMetadata);
        Assert.assertNotEquals(collectionResponseEnvelope.getCollectionResponsePaging(), oldPagingMetadata);

        Assert.assertEquals(collectionResponseEnvelope.getCollectionResponse(), newResponses);
        Assert.assertEquals(collectionResponseEnvelope.getCollectionResponseCustomMetadata(), newResponseMetadata);
        Assert.assertEquals(collectionResponseEnvelope.getCollectionResponsePaging(), newResponsesPaging);
        break;
      case CREATE_COLLECTION:
        BatchCreateResponseEnvelope batchCreateResponseEnvelope = (BatchCreateResponseEnvelope)responseEnvelope;
        List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> oldCreateResponses =
            batchCreateResponseEnvelope.getCreateResponses();

        CreateIdStatus<String> newCreateIdStatus = new CreateIdStatus<>(HttpStatus.S_201_CREATED.getCode(),
                                                                        "key",
                                                                        null,
                                                                        AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion());
        RestLiServiceException newException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        BatchCreateResponseEnvelope.CollectionCreateResponseItem successCreateItem
            = new BatchCreateResponseEnvelope.CollectionCreateResponseItem(newCreateIdStatus);
        BatchCreateResponseEnvelope.CollectionCreateResponseItem exceptionCreateItem
            = new BatchCreateResponseEnvelope.CollectionCreateResponseItem(newException);

        List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> newCreateResponses =
            Arrays.asList(successCreateItem, exceptionCreateItem);

        batchCreateResponseEnvelope.setCreateResponse(newCreateResponses, HttpStatus.S_200_OK);

        Assert.assertNotEquals(batchCreateResponseEnvelope.getCreateResponses(), oldCreateResponses);
        Assert.assertEquals(batchCreateResponseEnvelope.getCreateResponses(), newCreateResponses);

        BatchCreateResponseEnvelope.CollectionCreateResponseItem successItem =
            batchCreateResponseEnvelope.getCreateResponses().get(0);
        Assert.assertEquals(successItem.getRecord(), newCreateIdStatus);
        Assert.assertEquals(successItem.getId(), "key");
        Assert.assertFalse(successItem.isErrorResponse());
        Assert.assertNull(successItem.getException());
        Assert.assertEquals(successItem.getStatus(), HttpStatus.S_201_CREATED);

        BatchCreateResponseEnvelope.CollectionCreateResponseItem errorItem =
            batchCreateResponseEnvelope.getCreateResponses().get(1);
        Assert.assertNull(errorItem.getRecord());
        Assert.assertNull(errorItem.getId());
        Assert.assertTrue(errorItem.isErrorResponse());
        Assert.assertEquals(errorItem.getException(), newException);
        Assert.assertEquals(errorItem.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        break;
      case BATCH_ENTITIES:
        BatchResponseEnvelope batchResponseEnvelope = (BatchResponseEnvelope)responseEnvelope;
        Map<?, BatchResponseEnvelope.BatchResponseEntry> oldBatchResponses =
            batchResponseEnvelope.getBatchResponseMap();

        RecordTemplate newResponseRecord = new EmptyRecord();
        RestLiServiceException newResponseException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        Map<String, BatchResponseEnvelope.BatchResponseEntry> newBatchResponses =
            new HashMap<String, BatchResponseEnvelope.BatchResponseEntry>();
        newBatchResponses.put("id1", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK,
                                                                                  newResponseRecord));
        newBatchResponses.put("id2",
                              new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                                           newResponseException));


        batchResponseEnvelope.setBatchResponseMap(newBatchResponses, HttpStatus.S_200_OK);

        Map<?, BatchResponseEnvelope.BatchResponseEntry> envelopeMap = batchResponseEnvelope.getBatchResponseMap();
        Assert.assertNotEquals(envelopeMap, oldBatchResponses);
        Assert.assertEquals(envelopeMap, newBatchResponses);

        BatchResponseEnvelope.BatchResponseEntry id1Entry = envelopeMap.get("id1");
        Assert.assertEquals(id1Entry.getStatus(), HttpStatus.S_200_OK);
        Assert.assertEquals(id1Entry.getRecord(), newResponseRecord);
        Assert.assertFalse(id1Entry.hasException());
        Assert.assertNull(id1Entry.getException());

        BatchResponseEnvelope.BatchResponseEntry id2Entry = envelopeMap.get("id2");
        Assert.assertEquals(id2Entry.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        Assert.assertNull(id2Entry.getRecord());
        Assert.assertTrue(id2Entry.hasException());
        Assert.assertEquals(id2Entry.getException(), newResponseException);
        break;
      case STATUS_ONLY:
        // status only envelopes are blank by default since they have no data fields
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @Test(dataProvider = "resourceMethodProvider")
  public void testEnvelopeSetDataNull(ResourceMethod resourceMethod)
  {
    // create an envelope and set all the data to null
    RestLiResponseEnvelope responseEnvelope = buildBlankResponseEnvelope(resourceMethod);
    responseEnvelope.clearData();
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);

    // extract the correct response envelope based on the data type and verify the data fields are all null
    switch (responseType)
    {
      case SINGLE_ENTITY:
        RecordResponseEnvelope recordResponseEnvelope = (RecordResponseEnvelope)responseEnvelope;
        Assert.assertNull(recordResponseEnvelope.getRecord());
        break;
      case GET_COLLECTION:
        CollectionResponseEnvelope collectionResponseEnvelope = (CollectionResponseEnvelope)responseEnvelope;
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponse());
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponseCustomMetadata());
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponsePaging());
        break;
      case CREATE_COLLECTION:
        BatchCreateResponseEnvelope batchCreateResponseEnvelope =
            (BatchCreateResponseEnvelope)responseEnvelope;
        Assert.assertNull(batchCreateResponseEnvelope.getCreateResponses());
        break;
      case BATCH_ENTITIES:
        BatchResponseEnvelope batchResponseEnvelope = (BatchResponseEnvelope)responseEnvelope;
        Assert.assertNull(batchResponseEnvelope.getBatchResponseMap());
        break;
      case STATUS_ONLY:
        // status only envelopes don't have data fields
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @DataProvider
  private Object[][] envelopeResourceMethodDataProvider()
  {
    ResourceMethod[] resourceMethods = ResourceMethod.values();
    Object[][] envelopeResourceMethods = new Object[resourceMethods.length][2];
    for (int i = 0; i < resourceMethods.length; i++)
    {
      RestLiResponseEnvelope responseEnvelope = buildBlankResponseEnvelope(resourceMethods[i]);
      envelopeResourceMethods[i][0] = responseEnvelope;
      envelopeResourceMethods[i][1] = resourceMethods[i];
    }
    return envelopeResourceMethods;
  }

  private static RestLiResponseEnvelope buildBlankResponseEnvelope(ResourceMethod resourceMethod)
  {
    switch (resourceMethod)
    {
      case GET:
        return new GetResponseEnvelope(HttpStatus.S_200_OK, new EmptyRecord());
      case CREATE:
        return new CreateResponseEnvelope(HttpStatus.S_200_OK, new EmptyRecord(), false);
      case ACTION:
        return new ActionResponseEnvelope(HttpStatus.S_200_OK, new EmptyRecord());
      case GET_ALL:
        return new GetAllResponseEnvelope(HttpStatus.S_200_OK, Collections.emptyList(), null, new EmptyRecord());
      case FINDER:
        return new FinderResponseEnvelope(HttpStatus.S_200_OK, Collections.emptyList(), null, new EmptyRecord());
      case BATCH_CREATE:
        return new BatchCreateResponseEnvelope(HttpStatus.S_200_OK, Collections.emptyList(), false);
      case BATCH_GET:
        return new BatchGetResponseEnvelope(HttpStatus.S_200_OK, Collections.emptyMap());
      case BATCH_UPDATE:
        return new BatchUpdateResponseEnvelope(HttpStatus.S_200_OK, Collections.emptyMap());
      case BATCH_PARTIAL_UPDATE:
        return new BatchPartialUpdateResponseEnvelope(HttpStatus.S_200_OK, Collections.emptyMap());
      case BATCH_DELETE:
        return new BatchDeleteResponseEnvelope(HttpStatus.S_200_OK, Collections.emptyMap());
      case PARTIAL_UPDATE:
        return new PartialUpdateResponseEnvelope(HttpStatus.S_200_OK);
      case UPDATE:
        return new UpdateResponseEnvelope(HttpStatus.S_200_OK);
      case DELETE:
        return new DeleteResponseEnvelope(HttpStatus.S_200_OK);
      case OPTIONS:
        return new OptionsResponseEnvelope(HttpStatus.S_200_OK);
      default:
        throw new IllegalStateException();
    }
  }
}
