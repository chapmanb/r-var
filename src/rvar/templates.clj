(comment "
  HTML templates for web page display.
")

(ns rvar.templates
  (:use [hiccup.core]
        [hiccup.page-helpers :only [link-to]]
        [hiccup.form-helpers :only [form-to file-upload]]
        [com.reasonr.scriptjure :as scriptjure]
        [rvar.variant]
        [rvar.model]
        [rvar.external])
  (:require [appengine.users :as users]
            [clojure.contrib.str-utils2 :as str2]
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
               [:#header-logo :float "left" :margin-right "10px"
                :margin-bottom "20px" :margin-top "20px"]
               [:#header-title :float "left" :vertical-align "center"
                :margin-top "20px"])]))

(defn upload-genome []
  "Provide a form to upload 23andMe genomic information."
  (form-to {:enctype "multipart/form-data"} [:post "/upload/23andme"]
      [:fieldset
       [:legend "Upload 23andMe data"]
       [:ul
       ;[:label (:for :ufile) "Data file"]
        (file-upload :ufile)]
       [:button (:type "submit") "Process"]]))

(defn health-template [request]
  "Provide entry points for exploring SNPs related to phenotypes."
  (let [params (:query-params request)
        std-ol (list :list-style-type "none" :margin 0 :padding 0)
        std-li (list :margin "3px" :padding "0.4em" :font-size "1.4em" :height "18px")]
    [:div {:class "container"}
     [:style {:type "text/css"}
      (gaka/css [:#health-select std-ol :width "100%" 
                 [:li std-li]]
                [:#vrn-select std-ol :width "100%" 
                 [:li std-li]])]
     [:script {:type "text/javascript" :src "/static/js/rvar/health.js"}]
     [:div {:class "span-8"}
       [:h4 "&nbsp;"]
       [:ol {:id "health-select"}
        (for [p (get-phenotypes)]
          [:li {:class "ui-widget-content"} p])]]
     [:div {:class "span-12 last"}
       [:h3 {:id "vrn-header"} "Select a health topic to explore"]
       [:ol {:id "vrn-select"}]]
     [:div {:class "span-6" :id "back-page"}]
     [:div {:class "span-6 last" :id "for-page"}]
     [:input {:type "hidden" :id "cur-phn" :value (get params "phenotype" "")}]
     [:input {:type "hidden" :id "cur-start" :value (get params "start" "0")}]
     [:input {:type "hidden" :id "cur-limit" :value (get params "limit" "10")}]
     ]))

(defn- disqus-thread [identifier sname custom-js]
  [:div {:id "disqus_thread"}
   [:script {:type "text/javascript"}
    (str 
     "var disqus_identifier = '" identifier "';"
     "var disqus_developer = location.host.match(/^localhost/) ? 1 : 0;
      var disqus_callback = function () { 
        $('.dsq-request-user-name > a').each(function() {
        });
        $('.dsq-request-user-logout').click(function () {
        });
        $('.dsq-login-button').find('a').click(function () {
        });
     " custom-js "
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
  (let [sname "r-var"
        vrn (-> request (:query-params) (get "vrn"))
        std-ol (list :list-style-type "none" :margin 0 :padding 0)
        std-li (list :margin "3px" :padding "0.4em" :font-size "1.4em" :height "18px")]
    [:div {:class "container"}
     [:script {:type "text/javascript"
               :src "/static/js/rvar/variation.js"}]
     [:style {:type "text/css"}
      (gaka/css [:#vrn-phenotypes std-ol :width "100%" 
                 [:li std-li]])]
     [:h3 vrn]
     [:div {:class "span-14" :id "genes"}
      [:h4 "Genes"]
      [:ul
       (for [[gname gdesc allele mod-details] (vrn-gene-changes vrn)]
         [:li (str2/join " " [gname gdesc allele])
         [:ul
         (for [[cmod cmod-details] mod-details]
           [:li (str2/join " " [cmod cmod-details])])]])]]
     [:div {:class "span-5" :id "phenotypes"}
      [:ul {:id "vrn-phenotypes"}
       (for [phn (get-vrn-phenotypes vrn)]
         [:li {:class "ui-widget-content"} phn])]]
     [:div {:class "span-4 last" :id "vrn-links"}
      [:ul
       (for [link (vrn-links vrn)]
         [:li link])]]
     [:div {:class "span-23 last"}
       (disqus-thread vrn sname "")]]))
     ;(disqus-body-end sname)]))

(defn personal-template [request]
  "Information for a logged in users personal page."
  (let [sname "r-var"
        custom-js "$('#dsq-global-toolbar').hide();
                   $('.dsq-options').hide();
                   $('#dsq-comments-title').hide();
                   $('#dsq-comments').hide();
                   $('#dsq-pagination').hide();
                   $('#dsq-new-post').find('h3').hide();
                   $('.dsq-autheneticate-copy').html('Login');
                   $('#dsq-form-area').hide();"]
     (disqus-thread "personal" sname custom-js)))
  ;(let [ui (users/user-info)]
  ;  (if (:user ui)
  ;    [:html
  ;      [:script {:type "text/javascript"
  ;                :src "/static/js/rvar/variation.js"}]
  ;      [:table {:id "var-grid"}]
  ;      (upload-genome)]
  ;    [:html "Please "
  ;     (link-to (.createLoginURL (:user-service ui) "/") "login")
  ;     " to add your personal genome information."])))

(defn landing-template [request]
  [:div {:id "overview" :class "container span-23 last"}
   [:script {:type "text/javascript" :src "/static/js/rvar/landing.js"}]
   [:div {:class "span-21 last"}
     [:p "Our genomes are a wonderful array of unique variations.
          Access to our own personal sequences gives us the tools
          to explore individual responses to disease and everyday life."]]
  [:div {:class "span-7"} "Inform yourself about genetic variability."]
  [:div {:class "span-7"} "Share your knowledge of how a variation influences your life and treatment."]
  [:div {:class "span-7 last"} "Discover approaches you could take to improve your health, based on your personal genome."]
  [:div {:id "getting-started" :class "span-10 last"}
    [:a {:href "/health"} "Choose a health topic of interest"]]])

(defn index-template [request]
  "Main r-var display page."
  (let [title "Ourvar: exploring our genomic variability"]
    [:html
     [:head (std-header title)
      [:script {:type "text/javascript"}
       (scriptjure/js (.ready ($ document)
          (fn [] (.tabs ($ "#nav-tabs") {:cookie {:expires 1}})
            (.button ($ "#user-manage a")))))]
     [:body 
      [:div {:class "container"}
       ;(user-info request)
       [:div {:id "header" :class "span-24 last"}
        [:div {:id "header-logo"}
          [:img {:src "/static/images/aardvark.jpg" :width "120" :height "60"}]]
        [:div {:id "header-title"}
          [:h2 title]]]]
      [:div {:class "container"}
       [:div {:class "span-24 last" :id "content"}
        [:div {:id "nav-tabs"}
         [:ul
          [:li (link-to "#overview" "Overview")]
          [:li (link-to "/health" "Health")]
          [:li (link-to "/varview" "Variations")]
          [:li (link-to "/personal" "Personal")]]
         (landing-template request)]]]]]]))
