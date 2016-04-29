namespace * ophan.thrift.nativeapp

include "ads.thrift"
include "benchmark.thrift"

enum EventType {
   /**
    * When a user views a 'page'.
    **/
   VIEW,

   /**
   * An ad has been loaded
   **/
   AD_LOAD,

    /**
     * The event contains performance benchmark data.
     **/
    PERFORMANCE,

    /**
     * The event contains profiling data for network based operations.
     **/
    NETWORK,
}

/**
 * The means by which the user arrived at a page.
 *
 * As well as the capitalised versions listed below, these values
 * can be supplied in json as camelCase, e.g. inAppLink
 */
enum Source {

    /**
    * User clicked an link on a front or section page
    **/
    FRONT_OR_SECTION = 0,

   /**
    * user clicked a link on a fixtures page
    */
    FIXTURES_PAGE = 1,

    /**
    * User swiped across the screen
    **/
    SWIPE = 2,

    /**
    * User clicked a link within an article
    **/
    IN_ARTICLE_LINK = 3,

   /**
    * Whether the user clicked on a Guardian link (anywhere on the device) and chose to open it using our native app.
    */
    EXTERNAL_LINK = 4,

   /**
    * Whether the user clicked on a link in the 'More on this story' component.
    */
    RELATED_ARTICLE_LINK = 5,

   /**
    * Whether the user came to the page via a push notification.
    * The id can be stored in Event.pushNotificationId.
    */
    PUSH = 6,

    /**
    * meaning tbc
    **/
    HANDOFF_WEB = 7,

    /**
    * meaning tbc
    **/
    HANDOFF_APP = 8,

    /**
    * user clicked a link from a notification centre / home page widget
    **/
    WIDGET = 9,

    /**
    * meaning tbc
    **/
    RESUME_MEDIA = 10,

    /**
    * user clicked the back button
    **/
    BACK = 11,

    /**
    * meaning tbc
    **/
    SEARCH = 12
}


/**
* What type of subscription the user has (or had) for the app
**/
enum SubscriptionType {

  /**
   * No subscription in place, and no previous record of a subscription on this device.
   **/
  FREE = 1,

  /**
   * User has an active subscription via the device's store.
   *
   * In json, "play" or "apple" are supported as synonyms of this value.
   **/
  STORE = 2,

  /**
   * User as an active subscription as part of a print bundle
   **/
  PRINT = 4,

  /**
   * No subscription in place, but user did previously have a subscription via the
   * device's store which is now expired.
   *
   * In json, "play:expired" and "apple:expired" are supported as synonyms of this value.
   **/
  FREE_WITH_EXPIRED_STORE = 3,


  /**
   * No subscription in place, but user did previously have a subscription as
   * part of a print bundle which is now expired.
   *
   * In json, "print:expired" is supported as a synonym of this value.
   **/
  FREE_WITH_EXPIRED_PRINT = 5,

}


/**
 * E.g. a 'page view' see EventType.
 **/
struct Event {

   /**
    * The type of this event
    */
    3: optional EventType eventType = EventType.VIEW;

   /**
     * Unique id associated with this specific event.
     *
     * You must make sure this is globally unqiue: ophan will only process one event per eventId.
     */
    1: required string eventId;

   /**
     * The id of this page view. Defaults to the same as event Id which is fine for events of type View.
     * However, AD_LOAD events must set this to be the same as the viewId of the of the page view on which
     * this ad is shown.
     */
   9: optional string viewId;

   /**
     * This is for reporting offline events.
     *
     * The number of milliseconds ago that the event occured. (We deliberately don't
     * use an absolute timestamp to avoid issues with clocks on mobile devices being incorrect.)
     *
     * This number should be zero or a positive number, never negative (that would mean in the future!).
     *
     * If an event has just happened, set this value to 0.
     */
   2: optional i32 ageMs = 0;

   /**
    * Represents the page that has been displayed.
    * For content pages, this should the exact content api path with a "/" prefix.
    * For other pages, this should be the path of the corresponding web page on theguardian.com.
    *
    * This is mandatory if eventType is VIEW.
    */
    4: optional string path;

   /**
    * The referring path, i.e. the path representing a page displyed on the app on
    * which the user clicked a link to arrive at this page.
    */
    5: optional string previousPath;

   /**
    * The means by which the user arrived at this page.
    */
    6: optional Source referringSource;

   /**
    * An id which we can link back to Pushy.
    */
    7: optional string pushNotificationId;

    /**
    * Details about a rendered ad.
    * Only applicable if eventType is AD_LOAD.
    **/
    8: optional ads.RenderedAd adLoad;

    /**
    * Contains benchmark data.
    * Only applicable if eventType is PERFORMANCE.
    */
    10: optional benchmark.BenchmarkData benchmark;

    /**
    * Contains performance data for network based operations.
    * Only applicable if eventType is NETWORK.
    */
    11: optional benchmark.NetworkOperationData networkOperation;

}

/**
 * A specific version of the app exisits for each edition in the content api.
 * For example, US visitors get the US version of our app.
 */
enum Edition {
    UK,
    US,
    AU
}

/**
 * Details about this running application
 */
struct App {

   /**
    * The version of the app.
    */
    1: optional string version;

   /**
    * The device family.
    */
    2: optional string family;

   /**
    * The device's os.
    */
    3: optional string os;

   /**
    * The edition of the app.
    */
    4: optional Edition edition;
}

struct Device {

    1: optional string name;

    2: optional string manufacturer;
}


/**
* This is the root object that represents a tracking submission from native apps.
*
* This can be supplied to ophan in one of two ways:
*
* <ol>
*   <li>Create the equivalent json and POST the json to https://ophan.theguardian.com/mob</li>
*   <li>Create a thift binary blob in compact binary protocol format from
*   <a href="https://github.com/guardian/ophan/blob/master/event-model/src/main/thrift/nativeapp.thrift">this definition</a>
*   and POST to
*   https://ophan.theguardian.com/mob_thrift. (NB: at time of writing this isn't currently implemented,
*   but will be soon - talk to Graham Tackley.)</li>
* </ol>
*
* Note that, for largely backwards compatibility reasons, in some cases we allow synonyms for enum values in
* json; these are noted in the descriptions below.
**/
struct NativeAppSubmission {

   /**
    * App specific information.
    */
    2: required App app;

   /**
    * Device specific information.
    */
    3: optional Device device;

   /**
    * Equivalent to a web cookie. A way of identifying unique devices.
    */
    4: required string deviceId;

   /**
    * The user’s guardian user id if they are logged in.
    */
    5: optional string userId;

   /**
    * What type of subscription does this user have?
    */
    6: optional SubscriptionType subscriptionId;

    /**
    * The interaction events contained within this submission.
    **/
    7: required list<Event> events;
}
