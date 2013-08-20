package org.usergrid.rest.applications.collection.activities;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.org.apache.xml.internal.security.transforms.implementations.TransformBase64Decode;
import org.apache.commons.lang.ArrayUtils;
import org.aspectj.lang.annotation.Before;
import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.junit.Test;
import org.usergrid.rest.AbstractRestIT;
import org.usergrid.rest.TestContextSetup;
import org.usergrid.rest.test.resource.CustomCollection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.usergrid.utils.MapUtils.hashMap;

/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class PagingEntitiesTest extends AbstractRestIT {

  @Rule
  public TestContextSetup context = new TestContextSetup(this);

  @Before("instantias the setup for the bests below")
  public void setUp() {

    CustomCollection activities = context.collection("activities");

    long created = 0;
    int maxSize = 5;
    long[] verifyCreated = new long[maxSize];
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();


    props.put("actor", actor);
    props.put("verb", "go");
    Map location = hashMap("latitude",37);
    location.put("longitude",-75);
    props.put ("location",location);

    for (int i = 0; i < maxSize; i++) {

      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      verifyCreated[i] = activity.findValue("created").getLongValue();
      if (i == 0) {
        created = activity.findValue("created").getLongValue();
      }
    }
    ArrayUtils.reverse(verifyCreated);

  }

  @Test //USERGRID-266
  public void pageThroughConnectedEntities() {

    CustomCollection activities = context.collection("activities");

    long created = 0;
    int maxSize = 20;
    long[] verifyCreated = new long[maxSize];
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();


    props.put("actor", actor);
    props.put("verb", "go");

    for (int i = 0; i < maxSize; i++) {

      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      verifyCreated[i] = activity.findValue("created").getLongValue();
      if (i == 0) {
        created = activity.findValue("created").getLongValue();
      }
    }
    ArrayUtils.reverse(verifyCreated);
    String query = "select * where created >= " + created;


    JsonNode node = activities.withQuery(query).withLimit(2).get();//activities.query(query,"limit","2");
    int index = 0;
    while (node.get("entities").get(0) != null) {
      assertEquals(2, node.get("entities").size());

      if (node.get("cursor") != null)
        node = activities.withQuery(query).withCursor(node.get("cursor").getTextValue()).withLimit(2).get();

      else
        break;

    }

  }

  @Test //USERGRID-1253
  public void pagingQueryReturnCorrectResults() throws Exception {

    CustomCollection activities = context.collection("activities");

    long created = 0;
    int maxSize = 23;
    long[] verifyCreated = new long[maxSize];
    Map actor = hashMap("displayName", "Erin");
    Map props = new HashMap();

    props.put("actor", actor);
    props.put("content", "bragh");

    for (int i = 0; i < maxSize; i++) {

      if (i > 17 && i < 23)
        props.put("verb", "stop");
      else
        props.put("verb", "go");
      props.put("ordinal", i);
      JsonNode activity = activities.create(props);
      verifyCreated[i] = activity.findValue("created").getLongValue();
      if (i == 18) {
        created = activity.findValue("created").getLongValue();
      }
    }

    String query = "select * where created >= " + created + " or verb = 'stop'";

    JsonNode node = activities.withQuery(query).get();

    for (int index = 0; index < 5; index++)
      assertEquals(verifyCreated[maxSize - 1 - index], node.get("entities").get(index).get("created").getLongValue());

    int totalEntitiesContained = activities.countEntities(query);

    assertEquals(5, totalEntitiesContained);
  }

  @Test //USERGRID-911
  public void pageIncorrectSelectCursor() {

    CustomCollection activities = context.collection("activities");

    setUp();

    String query = "  select *   ";//where created >= " + created;


    JsonNode node = activities.withQuery(query).withLimit(1).get();//activities.query(query,"limit","2");
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor("bacon%" + node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

    String incorrectCursor = node.get("cursor").getTextValue();
    incorrectCursor = '%' + incorrectCursor.substring(1);
    /*surround in try catch block*/
    try {
      if (node.get("cursor") != null) {
        node = activities.withQuery(query).withCursor(incorrectCursor).withLimit(1).get();
      }
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }
  }

  @Test //USERGRID-911
  public void pageCorrectSelectCursor() {

    CustomCollection activities = context.collection("activities");

    setUp();

    String query = "  select *   ";//where created >= " + created;


    JsonNode node = activities.withQuery(query).withLimit(1).get();//activities.query(query,"limit","2");
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor(node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }
  }


  @Test
  public void pageIncorrectSelectOrderSingleCursor() {

    CustomCollection activities = context.collection("activities");

    setUp();

    String query = " select *  order by created";


    JsonNode node = activities.withQuery(query).withLimit(1).get();
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor("dddx" + node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

    String incorrectCursor = node.get("cursor").getTextValue();
    incorrectCursor = '%' + incorrectCursor.substring(1);
    /*surround in try catch block*/
    try {
      if (node.get("cursor") != null) {
        node = activities.withQuery(query).withCursor(incorrectCursor).withLimit(1).get();
      }
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

  }

  @Test
  public void pageCorrectSelectOrderSingleCursor() {

    CustomCollection activities = context.collection("activities");

    setUp();
    String query = " select *  order by created";


    JsonNode node = activities.withQuery(query).withLimit(1).get();
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor(node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

  }

  @Test
  public void pageIncorrectSelectOrderDoubleCursor() {

    CustomCollection activities = context.collection("activities");

    setUp();
    String query = " select *  order by created,modified";


    JsonNode node = activities.withQuery(query).withLimit(1).get();
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor("dddx" + node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

    String incorrectCursor = node.get("cursor").getTextValue();
    incorrectCursor = '%' + incorrectCursor.substring(1);
    /*surround in try catch block*/
    try {
      if (node.get("cursor") != null) {
        node = activities.withQuery(query).withCursor(incorrectCursor).withLimit(1).get();
      }
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

  }

  /*didn't make an incorrect version of this because
  the cursor was the exact same as the single orderby*/

  @Test
  public void pageCorrectSelectOrderDoubleCursor() {

    CustomCollection activities = context.collection("activities");

    setUp();
    String query = " select *  order by created,modified";


    JsonNode node = activities.withQuery(query).withLimit(1).get();
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor(node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

  }

  @Test
  public void pageCorrectSelectStringCursor() {

    CustomCollection activities = context.collection("activities");
    setUp();
    String query = " select * where verb = 'go'";


    JsonNode node = activities.withQuery(query).withLimit(1).get();
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor(node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

  }

  @Test
  public void pageIncorrectSelectStringCursor() {

    CustomCollection activities = context.collection("activities");
    setUp();
    String query = " select * where verb = 'go'";


    JsonNode node = activities.withQuery(query).withLimit(1).get();
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor("%@T^" + node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

    String incorrectCursor = node.get("cursor").getTextValue();
    incorrectCursor = '%' + incorrectCursor.substring(1);
    /*surround in try catch block*/
    try {
      if (node.get("cursor") != null) {
        node = activities.withQuery(query).withCursor(incorrectCursor).withLimit(1).get();
      }
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

  }

  @Test
  public void pageIncorrectSelectLocationCursor() {

    CustomCollection activities = context.collection("activities");
    setUp();
    String query = " select * where location within 5 of 37,-75";


    JsonNode node = activities.withQuery(query).withLimit(1).get();
    int index = 0;
    try {
      while (node.get("entities").get(0) != null) {
        assertEquals(1, node.get("entities").size());

        if (node.get("cursor") != null)
          node = activities.withQuery(query).withCursor("%@T^" + node.get("cursor").getTextValue()).withLimit(1).get();

        else
          break;

      }
    /* What should this error message be?*/
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

    String incorrectCursor = node.get("cursor").getTextValue();
    incorrectCursor = '%' + incorrectCursor.substring(1);
    /*surround in try catch block*/
    try {
      if (node.get("cursor") != null) {
        node = activities.withQuery(query).withCursor(incorrectCursor).withLimit(1).get();
      }
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid Cursor", iae.getMessage());
    }

  }





}