(comment "
  HTML templates for web page display.
")

(ns rvar.templates
  (:use [clojure.contrib.json :as json]
        [hiccup.core]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to file-upload]]
        [com.reasonr.scriptjure :as scriptjure]
        [rvar.model])
  (:require [appengine.users :as users]
            [gaka [core :as gaka]]))

(defn user-info [request]
  (let [ui (users/user-info)]
  [:div {:class "span-24 last"}
    (if-let [user (:user ui)]
      [:div {:id "user-manage"}
       (link-to (.createLogoutURL (:user-service ui) "/") (.getEmail user))]
      [:div {:id "user-manage"}
       (link-to (.createLoginURL (:user-service ui) "/") "Login")]
      )
   ]))

(defn side-bar [request]
  "Common navigation and user management side bar."
  (let [ui (users/user-info)]
  [:div {:class "span-6 last" :id "sidebar"}
   [:h3 {:class "caps"} "User"]
   [:div {:class "box"}
    (if-let [user (:user ui)]
      [:div {:class "quiet"}
       (.getEmail user) "<br/>" (link-to (.createLogoutURL (:user-service ui) "/") "Logout")]
      [:div {:class "quiet"}
       (link-to (.createLoginURL (:user-service ui) "/") "Login")]
      )]
   ]))

(defn std-header [title]
  "Shared header elements between pages."
  (html
    [:title title]
    [:script {:type "text/javascript"
              :src "/static/js/jquery-1.4.2.min.js"}]
    [:script {:type "text/javascript" 
              :src "/static/js/jquery-ui-1.8.4.custom.min.js"}]
    [:script {:type "text/javascript" 
              :src "/static/js/jquery.cookie.js"}]
    [:script {:type "text/javascript" 
              :src "/static/js/grid.locale-en.js"}]
    [:script {:type "text/javascript" 
              :src "/static/js/jquery.jqGrid.min.js"}]
    [:link {:type "text/css" :rel "stylesheet" :media "screen" 
            :href "/static/css/Aristo/jquery-ui-1.8rc3.custom.css"}]
    [:link {:type "text/css" :rel "stylesheet" :media "screen" 
            :href "/static/css/ui.jqgrid.css"}]
    [:link {:type "text/css" :rel "stylesheet" :media "screen" 
            :href "/static/css/blueprint/screen.css"}]
    [:link {:type "text/css" :rel "stylesheet" :media "print" 
            :href "/static/css/blueprint/print.css"}]
    "<!--[if IE]>"
    [:link {:type "text/css" :rel "stylesheet" :media "screen" 
            :href "/static/css/blueprint/ie.css"}]
    "![endif]-->"
    [:style {:type "text/css"}
     (gaka/css [:#user-manage :float "right"]
               [:#header-logo :float "left" :margin-right "10px" :margin-bottom "20px"]
               [:#header-title :float "left" :vertical-align "center"])]))

(defn upload-genome []
  "Provide a form to upload 23andMe genomic information."
  (form-to {:enctype "multipart/form-data"} [:post "/upload/23andme"]
      [:fieldset
       [:legend "Upload 23andMe data"]
       [:ul
       ;[:label (:for :ufile) "Data file"]
        (file-upload :ufile)]
       [:button (:type "submit") "Process"]]))

(defn clean-db-items [maps]
  "Remove db-specific keys and parents from a map."
  (for [cur-map maps]
    (-> cur-map
      (dissoc :key)
      (dissoc :parent))))

(defn var-list [request]
  "Produce a JSON list of variations for the current user."
  (let [user (.getEmail ((request :appengine/user-info) :user))
        params (:params request)
        rows (Integer/parseInt (get params "rows"))
        start (* rows (- 1 (Integer/parseInt (get params "page"))))
        vars (clean-db-items (get-variations user))
        cur-vars (take rows (drop start vars))]
    (println cur-vars)
    (json/json-str {:total (count cur-vars) :page (get params "page")
                    :records (count cur-vars)
                    :rows cur-vars})))

(defn personal-template [request]
  "Information for a logged in users personal page."
  (let [ui (users/user-info)]
    (if (:user ui)
      [:html
        [:script {:type "text/javascript"
                  :src "/static/js/rvar/variation.js"}]
        [:table {:id "var-grid"}]
        (upload-genome)]
      [:html "Please "
       (link-to (.createLoginURL (:user-service ui) "/") "login")
       " to add your personal genome information."])))

(defn health-template [request]
  "Provide entry points for exploring SNPs related to phenotypes."
  [:div
   [:style {:type "text/css"}
    (gaka/css [:#selectable :list-style-type "none" :margin 0 :padding 0 :width "50%"
               [:li :margin "3px" :padding "0.4em" :font-size "1.4em" :height "18px"]])]
   [:script {:type "text/javascript"}
    (scriptjure/js 
      (.click (.children ($ "#selectable")) 
              (fn [] (.toggleClass ($ this) "ui-state-highlight")
                (.removeClass (.siblings ($ this)) "ui-state-highlight")))
      (.hover ($ "#selectable li") 
              (fn [] (.addClass ($ this) "ui-state-hover"))
              (fn [] (.removeClass ($ this) "ui-state-hover"))))]
   [:ol {:id "selectable"}
    (for [p (get-phenotypes)]
      [:li {:class "ui-widget-content" :value p} p])]])

(defn- disqus-thread [identifier sname]
  [:div {:id "disqus_thread"}
   [:script {:type "text/javascript"}
    (str 
     "var disqus_identifier = '" identifier "';"
     "var disqus_developer = location.host.match(/^localhost/) ? 1 : 0;
      var disqus_callback = function () { 
        $('.dsq-request-user-name > a').each(function() {
         console.info(this.getAttribute('href'));
         console.info(this.text);
        });
      };
      (function() {
       var dsq = document.createElement('script');
       dsq.type = 'text/javascript';
       dsq.async = true;"
       "dsq.src = 'http://" sname ".disqus.com/embed.js';"
       "(document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq);
       })();")]])

(defn- disqus-body-end [sname]
  [:script {:type "text/javascript"}
   (str
    "var disqus_shortname = '" sname "';"
    "(function () {
       var s = document.createElement('script'); s.async = true;"
       "s.src = 'http://disqus.com/forums/" sname "/count.js';"
       "(document.getElementsByTagName('HEAD')[0] || document.getElementsByTagName('BODY')[0]).appendChild(s);
    }());")])

(defn variation-template [request]
  "Show details and discussion for a specific variation."
  (let [sname "r-var"]
    [:div
     (disqus-thread "test" sname)
     (disqus-body-end sname)]))

(defn index-template [request]
  "Main r-var display page."
  (let [title "r-var: exploring our genomic variability"]
    [:html
     [:head (std-header title)
      [:script {:type "text/javascript"}
       (scriptjure/js (.ready ($ document)
          (fn [] (.tabs ($ "#tabs") {:cookie {:expires 1}})
            (.button ($ "#user-manage a")))))]
     [:body 
      [:div {:class "container"}
       (user-info request)
       [:div {:id "header" :class "span-24 last"}
        [:div {:id "header-logo"}
          [:img {:src "/static/images/aardvark.jpg" :width "120" :height "60"}]]
        [:div {:id "header-title"}
          [:h2 title]]]]
      [:div {:class "container"}
       [:div {:class "span-24 last" :id "content"}
        [:div {:id "tabs"}
         [:ul
          [:li (link-to "#overview" "Overview")]
          [:li (link-to "/health" "Health")]
          [:li (link-to "/varview" "Variations")]
          [:li (link-to "/personal" "Personal")]]
         [:div {:id "overview"}
          ""]]]]]]]))
