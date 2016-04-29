namespace * ophan.thrift.event

include "ua.thrift"
include "nativeapp.thrift"
include "ads.thrift"
include "quiz.thrift"


/**
* Indicates sites that we really care about, factoring out the variation
* on domain that they sometimes contain.
**/
enum SignificantSite {
  /**
  * The Guardian website, including when viewed on native apps and
  * for pages served on non www. subdomains such as membership.theguardian.com
  * and careers.theguardian.com
  **/
  GUARDIAN = 0;

  /** An email sent by the Guardian, either one of our scheduled emails
   * or by a user clicking "email this"  **/
  GUARDIAN_EMAIL = 1;

  /**
  * An app push alert sent by the Guardian
  **/
  GUARDIAN_PUSH = 15;

  /**
   * Any google website, regardless of subdomain and country domain
   * i.e. includes news.google.co.uk and www.google.ca
   * also includes google plus?
   **/
  GOOGLE = 2;

  /**
  * Twitter, including any where the source was a result of going via
  * the t.co redirector
  **/
  TWITTER = 3;

  /**
  * Facebook, including when we've inferrer that the referrer was facebook
  * because we were access inside a facebook native app.
  **/
  FACEBOOK = 4;

  REDDIT = 5;

  DRUDGE_REPORT = 6;

  /** Outbrain, both paid and non-paid **/
  OUTBRAIN = 7;

  TUMBLR = 8;

  PINTEREST = 9;

  DIGG = 10;

  STUMBLEUPON = 11;

  FLIPBOARD = 12;

  LINKEDIN = 13;

  BING = 14;

}


/**
* Represents a url either of a page served or a referrer
**/
struct Url {
    /** The full raw URL as provided to ophan, potentially including
     * query string and fragment identifier
     *
     *  Be careful about using the value of this field: almost certainly
     *  you want to use the combination of domain and path instead, which are cleared of
     *  non-significant variation.
     */
  1: required string raw;

  /** the host of this url with no parsing or normalisation performed.
   *
   *  Be careful about using the value of this field: almost certainly
   *  you want to use the combination of domain and path instead, which are cleared of
   *  non-significant variation.
   **/
  2: required string host;

  /**
   * the domain of this url.
   *
   * This is the host stripped down to one level below the top level domain.
   *
   * e.g.
   * www.theguardian.com => theguardian.com
   * news.google.co.uk => google.co.uk
   * membership.theguardian.com => theguardian.com
   **/
  6: required string domain;

  /**
  * The path served on the given host, without query strings or fragment identifiers.
  *
  * For guardian urls, processing is performed on the path -
  * especially for the native mobile apps - to try to make the url
  * match up with those on www.theguardian.com
  *
  * Path will always start with a /.
  **/
  3: required string path;

  /**
  * Indicates sites that we really care about, factoring out the variation
  * on domain that they sometimes contain.
  *
  * Note that were this url represents a page on a site that we don't consider significant,
  * this value will be unpopulated. You should probably use domain to aggregate in that case.
  **/
  5: optional SignificantSite site;

  /** Indicates whether this url was synthesised in some way by ophan */
  11: optional bool synthesised = false;
}


/** Ophan assigns various ids to things when necessary
 * (on Web, by dropping cookies). This struct indicates
 * the id and
 * whether we freshly assigned an id on this request.
 */
struct AssignedId {

    /** The actual id */
    1: required string id;

    /**Whether the id was generated and set for the
     * first time on this request. If undefined,
     * it's not known whether the id is a new one
     * or not.
     */
    2: optional bool isNew;
}



/**
* The platform that served this request to the reader.
**/
enum Platform {
    R2 = 0;

    NEXT_GEN = 1;

    IOS_NATIVE_APP = 2;

    ANDROID_NATIVE_APP = 3;

    /**
    * Served as a result of embedding a media item on a third party site.
    * Note therefore you should not typically include this as a guardian
    * "page view". **/
    EMBED = 4;

    MEMBERSHIP = 5;

    FACEBOOK_INSTANT_ARTICLE = 6;
}




struct IpAddress {
    /** The full value of the X-Forwarded-For header supplied.
     * Normally you'll want to ignore this and use the ip field below.
     * This is here just in case there's a bug in our logic of decoding
     * the XFF header, or we can gain more information from the rest of the
     * header.
     */
    1: required string xForwardedForHeader;

    /** The ip address of the client */
    2: optional string ip;
}


/** A geographical location */
struct GeoPoint {
    /** Latitude */
    1: required double lat;
    /** Longitude */
    2: required double lon;
}

/**
 *  Where this request was made from, derived by ip address lookup.
 */
struct GeoLocation {

    /** The resolved geolocation of the ip address */
    3: optional GeoPoint geo;

    /** The two letter country code of the ip address;
     *  Note that guardian internal traffic (within the Guardian offices)
     *  is assigned a country code of "GNM"
     */
    4: optional string countryCode;

    /** Human readable country name */
    5: optional string countryName;

    /** Human readable city name */
    6: optional string city;

    /** Human readable continent name */
    7: optional string continent;

    /** Administrative subdivisions, e.g. "Wales", "Berkshire" or "Alaska"  */
    8: optional list<string> subdivisions;
}


/**
* Details about the page that was served to the user
**/
struct Page {

    /** Url of the page served
     */
    1: required Url url;

    /** The values of all of the CMP and INTCMP parameters  */
    4: optional set<string> campaignCodes;

    /**
    * The platform that served this page.
    * Marked as optional because when new platforms are added and your thrift definition hasn't been
    * updated, this will become empty.
    * **/
    5: optional Platform platform;

    /**
    * The section id of the page.
    *
    * Network front pages (e.g. "/uk") are assigned to a section named "/" (a single slash).
    *
    * Note this is currently extracted from the url, though in the future more advanced
    * techiniques may be used to caluclate this data.
    *
    * Will always be undefined for non www.theguardian.com urls.
    **/
    6: optional string section;

    /**
    * For most content pages, the publication date of the content, in ISO date format "YYYY-MM-DD"
    * e.g. 2014-01-20
    *
    * Note this is currently extracted from the url, though in the future more advanced
    * techiniques may be used to calculate this data.
    *
    * Fronts will never have a publication date.
    *
    **/
    7: optional string publicationDate;


    /**
    * Returns some approximation of the content type, generated by the serving platform.
    *
    * Currently, next-gen produces a maximum of one entry whereas R2 generates a number of varying entries.
    * Native mobile apps produce nothing. There is little consistency between the strings that the
    * platforms report.
    *
    * As a result, you are strongly advised not to make use of this field unless you have a really
    * good reason to do so. It will likely be removed at some point in the future.
    **/
    8: optional set<string> contentTypes;


    /**
    * The set of component names that were rendered on this page. Currently only reported by next-gen,
    * this can be used to validate the effectiveness of particular components.
    **/
    9: optional set<string> renderedComponents;
}


/**
* Where the referrer was google and they've provided additional information
* on the query string, here is
* that additional information.
**/
struct GoogleReferral {

  /**
  * The query terms requested by the user
  **/
  1: optional string q;

  /**
  * The rank we were list at within the source, as indicated by the "cd" query
  * parameter.
  **/
  2: optional i32 rank;

  /**
  * The type of referral this was. Currently this is just a string, and is likely to
  * change as we overhaul our google ved parsing
  **/
  3: optional string source;


}



/**
* Represents the position and location of a link within the Guardian site.
* We hope to enhance this structure to include a better representation of what the links
* actually mean, but for now we just report exactly what the web site tells us,
* which is a hierarchical list of named items e.g.
*  "more","container-2 | highlights","Front | /uk"
*  "article","news | group-1+ | card-3","container-1 | headlines","Front | /uk"
**/
struct LinkName {

  /* List of link names, most specific first */
  1: optional list<string> raw;
}

/**
* Information about the referrer - previous page - that the reader navigated to
* this one from.
**/
struct Referrer {
    /**
    * The url of the referrer. The "raw" value in here is the value of
    * document.referrer as reported by the browser, or invented for mobile
    * apps.
    **/
    1: required Url url;

    /** the component that was clicked on for this referral,
     *  if the previous page was served by the guardian
     */
    4: optional string component;

    /**
    * The link name associated with the element clicked.
    */
    10: optional LinkName linkName;

    /** the platform of the referrer,
     *  if the previous page was served by the guardian
     */
    5: optional Platform platform;

    /** the viewId of the referrer,
     *  if the previous page was served by the guardian
     */
    6: optional string viewId;

    /** if this was from a guardian email, what email it was */
    7: optional string email;

    /** If this referral was from a native app, the source of the referral */
    8: optional nativeapp.Source nativeAppSource;

    /** If this referral was from google, and we have additional data on the query string,
    * the values we got.
    **/
    9: optional GoogleReferral google;
}


enum SuspectStatus {

    /** This event is valid and should be processed normally */
    VALID = 0;

    /** This event is invalid, becuase we think it was only sent because
     * of a persistent bug in the mobile apps where they erroneously
     * sent page view notifcation after a device suspend-resume
     */
    INVALID_APP_RESUME_BUG = 1;

    /**
    * This event does not represent a usual page view, as the user was returned
    * a non-200 sucess code. (Which we track by putting special ophan tracking on
    * the error page displayed.)
    *
    * Unless you're specifically interested in errors, you should ignore these as
    * page views.
    **/
    INVALID_WEB_NOT_SUCCESSFUL = 3;


    /**
    * This event was generated by robotic requests.
    *
    * Unless you're specifically interested in understanding javascript-enabled
    * robotic traffic, you should ignore these as page views.
    **/
    INVALID_ROBOT = 4;

    /**
    * This event was generated by display of a guardian media asset embedded on a
    * third party site.
    *
    * Unless you're specifically interested in understanding third party embed,
    * you should ignore these as page views.
    **/
    INVALID_THIRD_PARTY_EMBED = 5;

    /**
    * This event appears to have been generated by a user inside the Guardian network.
    *
    * Unless you're specifically interested in underestanding the behaviour of users
    * inside the Guardian networks, you should ignore these as page views.
    **/
    INVALID_INTERNAL_GUARDIAN_TRAFFIC = 6;
}


/**
 * Details about a page view - only populated for _PAGE_VIEW
 * event types
 **/
struct PageView {

    /** Whether we view this event as "suspect".
     * most consumers of this stream should ignore events
     * that are do not have a status of VALID.
     */
    1: optional SuspectStatus validity = SuspectStatus.VALID;

    /** Details about the page displayed */
    2: required Page page;

    /** The user agent that made this request */
    3: optional ua.UserAgent userAgent;

    /** Details about the location of the user made this request */
    4: optional GeoLocation location;

    /** IP details of the user who made this request **/
    7: optional IpAddress ipAddress;

    /** Details about the referrer to this page view
     *  Will only be present on _PAGE_VIEW events where
     *  a referer was received   */
    5: optional Referrer referrer;

    /** The http status returned to the user for this page view */
    6: required i16 httpStatus = 200;

    /**
    * The number of days in the previous week that the device has had a page view
    * recorded on the Guardian
    **/
    8: optional i32 daysVisitedInLastWeek;
}


struct AttentionTime {
  /**
  * Attention time spent on this page view (indicated by event.pageViewId)
  * in milliseconds.
  **/
  1: required i64 attentionMs;
}



struct AdInfo {
  /**
  * If present, indicates that we have detected the presence or absence of
  * ad blocking technology.
  *
  * If absent, this check was not performed. If we have previously sent an event
  * with this field present, you should treat that value as still valid.
  *
  * (Currently, the check is only performed on initial page load, so this field
  * will only ever be set in the event relating to the initial page view not on
  * subsequent events relating to that page view.)
  **/
  1: optional bool adBlockerDetected;

  /**
  * Details of one or more ads rendered. Currently, each ad rendering is sent to
  * ophan in a separate event so there will only ever be one entry in this list.
  * This is likely to change in the future, however.
  **/
  2: optional list<ads.RenderedAd> ads;

}


enum MediaType {
  VIDEO = 1;
  AUDIO = 2;
}


enum MediaEvent {
  /**
  * The media has been requested.
  * Currently, it appears this event is only received for pre-roll videos.
  **/
  REQUEST = 1;

  /**
  * The media is ready to play.
  **/
  READY = 2;

  /**
  * The media has started playing.
  **/
  PLAY = 3;

  /**
  * The media has played a quarter of the way through.
  * Currently, it appears that pre-roll videos do not send this event.
  **/
  PERCENT25 = 4;

  /**
  * The media has played half way though.
  * Currently, it appears that pre-roll videos do not send this event.
  **/
  PERCENT50 = 5;

  /**
  * The media has played three quarters of the way though.
  * Currently, it appears that pre-roll videos do not send this event.
  **/
  PERCENT75 = 6;

  /**
  * The media has played to the end.
  * NB: "END" is a reserved word in thrift apparently
  **/
  THE_END = 7;
}

/**
* Details about media playback progress
* Note: currently only guardian-hosted videos are reported.
**/
struct MediaPlayback {
  /**
  * The id of the media asset, e.g. gu-video-454297906
  * This matches up with the media id within the content api.
  **/
  1: required string mediaId;

  /**
  * The media type
  **/
  2: required MediaType mediaType;

  /**
  * If true, this event relates to the pre-roll (ad) of this media.
  * If false, this event reated to the core media content.
  **/
  3: required bool preroll;

  /**
  * The event type.
  **/
  4: required MediaEvent eventType;

}

/**
* Web performance data as captured from the browser performance timing api.
* In the descriptions below, "t" represents window.performance.timing
**/
struct WebPerformanceData {

  /**
  * Time in ms that dns lookup took.
  * Calculated by t.domainLookupEnd - t.domainLookupStart
  **/
  1: required i64 dns;

  /**
  * Time in ms that connection to the server took.
  * Calculated by t.connectEnd - t.connectStart
  **/
  2: required i64 connection;

  /**
  * Time to first byte
  * Calculated by t.responseStart - t.connectEnd
  **/
  3: required i64 firstByte;

  /**
  * First byte to last byte, or closed, including if from cache.
  * Calculated by t.responseEnd - t.responseStart
  **/
  4: required i64 lastByte;

  /**
  * From last byte of doc to start of domContentLoaded
  * Calculated by t.domContentLoadedEventStart - t.responseEnd
  **/
  5: required i64 domContentLoadedEvent;

  /**
  * domcontentLoaded to start of load event
  * Calculated by t.loadEventStart - t.domContentLoadedEventStart
**/
  6: required i64 loadEvent;

  /**
  * The navigation type
  * Value of window.performance.navigation.type
  **/
  7: required i64 navType;

  /**
  * Number of redirects on current domain.
  * Value of window.performance.navigation.redirectCount
  **/
  8: required i64 redirectCount;

}


struct AbTest {
  /**
  * the test that the user is participating in
  **/
  1: required string name;

  /**
  * the variant that they are seeing
  **/
  2: required string variant;
}


struct AbTestInfo {
  /**
  * On the left hand side of the map, .
  * On the right hand side of the map, the variant that they are seeing.
  **/
  1: required set<AbTest> tests;

}


struct Component {
  /**
  * The name of this component
  **/
  1: required string name;

  /**
  * How long, in milliseconds, that the component took to load.
  * If absent, the component had not completed loading by the time the snapshot of lazy
  * components was taken.
  **/
  2: optional i64 loadTimeMs;
}

struct LazyComponents {
  /**
  * The set of components that were loaded lazily on this page.
  * Note: statically loaded components are reported in Page.components
  **/
  1: required set<Component> components;
}


struct AltIds {

  /**
  * The omniture user identifier, stored in the s_vi coookie.
  * Note currently this is not available for native app events.
  * It is present on both web initial and web additional events.
  **/
  1: optional string s_vi;

  /**
  * The omniture session identifier, stored in the s_sess cookie.
  * Note currently this is not available for native app events.
  * It is present on both web initial and web additional events.
  **/
  2: optional string s_sess;

  /**
  * The krux identifer for this user.
  * Note currently this is not available for native app events.
  * It is present only on web initial events, i.e. events that also include a populated
  * pageView.
  **/
  3: optional string kruxId;
}


struct InPageClick {

    /** the component that contained the item clicked on
     */
    1: optional string component;

    /**
    * The link name associated with the element clicked.
    */
    2: optional LinkName linkName;

}

struct Event {

    /** Gloablly unique id associated with this event. Ophan never makes
     * better than at-least-once delivery promises, so you
     * must ensure that processing two events with the same
     * uniqueEventId has no effect
     */
    2: required string uniqueEventId;


    /** The date time (in millis since epoch UTC) at which this event
     * occurred.
     */
    3: required i64 dt;

    /**
    * The date time (in millis since epoc UTC) at which this event
    * was received by ophan for processing. For web generated events,
    * this is the same as dt. For native mobile app generated events, it
    * might not be.
    */
    10: required i64 receivedDt;


    /** The page view for which this event is associated.
     * Ophan may send multiple events relating to the same
     * page view, which may contain updates to any previously
     * supplied data or new data. You should treat the one with the
     * highest timestamp (dt) as the most accurate.
     */
    4: required string pageViewId;


    /** The unqiue id associated with this browser.
     * Currently this is maintained by setting a cookie for web
     * events, or otherwise determined for native apps.
     */
    5: required AssignedId browserId;


    /** The unique id associated with this "visit".
     * For web reports, the visit id is a refreshed session
     * cookie that expires after 30 minutes of activity.
     * Mobile apps do not currently set this value.
     */
    6: optional AssignedId visitId;


    /**
    * If the user is logged in, the identity user id.
    * **/
    7: optional string userId;

    /**
    * Various other identifiers we may have to identify this user.
    **/
    17: optional AltIds altIds;


    /**
    *  If populated, this event represents a page view.
    **/
    8: optional PageView pageView;

    /**
    * If populated, this event includes attention time data.
    * Note this will also be populated, typically with a value of zero,
    * alongside a pageView value for page views generated by platforms
    * that support attention time tracking.
    **/
    9: optional AttentionTime attention;

    /**
    * If populated, this event includes advertising-related information
    **/
    11: optional AdInfo ads;

    /**
    * If populated, this event includes web performance load information
    **/
    12: optional WebPerformanceData perf;

    /**
    * If populated, this event includes data about media playback
    **/
    13: optional MediaPlayback media;

    /**
    * If populated, this event includes data about ab tests that the user was a member of
    **/
    14: optional AbTestInfo ab;

    /**
    * If populated, this event includes data about components that were lazily loaded.
    **/
    15: optional LazyComponents lazyComponents;

    /**
    * If populated, this event includes data about a quiz event.
    **/
    16: optional quiz.QuizEvent quizEvent;

    /**
    * If populated, this event includes data about a click that did not result in a page transition
    **/
    18: optional InPageClick inPageClick;
}



