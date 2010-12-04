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
    [:link {:type "text/css" :rel "stylesheet" :media "screen" 
            :href "/static/css/Aristo/jquery-ui-1.8.5.custom.css"}]
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

(def std-footer
  [:div {:class "container" :id "footer"}
   [:div {:class "prepend-21 span-3 last"}
     [:a {:rel "license" :href "http://creativecommons.org/licenses/by/3.0/"}
      [:img {:alt "Creative Commons License" :style "border-width:0" 
             :src"http://i.creativecommons.org/l/by/3.0/88x31.png"}]]]])

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
        std-li (list :margin "1px" :padding "0.4em" :font-size "1.4em" :height "18px")]
    [:div {:class "container"}
     [:style {:type "text/css"}
      (gaka/css [:#health-select std-ol :width "100%" 
                 [:li std-li]]
                [:#vrn-select std-ol :width "100%" :clear "both"
                 [:li std-li]]
                [:#vrn-header :float "left" :margin-right "10px"]
                [:#vrn-header-page :float "left"]
                [:#vrn-less-button :float "left" :margin-right "20px"]
                [:#vrn-more-button :float "right"])]
     [:script {:type "text/javascript" :src "/static/js/rvar/health.js"}]
     [:div {:class "span-8"}
       [:h4 "&nbsp;"]
       [:ol {:id "health-select"}
        (for [p (get-phenotypes)]
          [:li {:class "ui-widget-content"} p])]]
     [:div {:class "span-12 last"}
      [:button {:id "vrn-less-button"}]
      [:h3 {:id "vrn-header"} "Select a health topic to explore"]
      [:div {:id "vrn-header-page"}]
      [:button {:id "vrn-more-button"}] ;:class "ui-icon ui-icon-circle-arrow-e"}]
      [:ol {:id "vrn-select"}]]
     [:input {:type "hidden" :id "cur-phn" :value (get params "phenotype" "")}]
     [:input {:type "hidden" :id "cur-start" :value (get params "start" "0")}]
     [:input {:type "hidden" :id "cur-limit" :value (get params "limit" "10")}]]))

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

(defn- gene-changes-template [vrn]
  "Display of gene changes associated with a variation"
  [:ul {:id "gene-allele"}
  (for [[allele gene-info] (vrn-gene-changes vrn)]
    [:li allele
    [:ul {:id "gene-name"}
     (for [[_ eid gname gdesc mod-details] gene-info]
       [:li (ensembl-gene-link eid (str2/join " " [gname gdesc]))
       [:ul {:id "gene-mods"}
        (for [[cmod cmod-details] mod-details]
          [:li (str2/join " " [cmod cmod-details])])]])]])])

(defn variation-template [request]
  "Show details and discussion for a specific variation."
  (let [sname "r-var"
        vrn (-> request (:query-params) (get "vrn" "rs6604026"))
        link-style (list [:a :text-decoration "none"] [:a:hover :color "#5f83b9"])
        link-portal-css (list 
                          [:ul :list-style-type "none" :margin 0 :padding 0]
                          [:li :margin "2px" :padding "0.4em" :font-size "1.2em" :height "15px"
                           link-style]
                          [:h4 :text-align "center"])]
    [:div {:class "container"}
     [:style {:type "text/css"}
      (gaka/css [:#vrn-pubs link-portal-css]
                [:#vrn-links link-portal-css]
                [:#vrn-links-info :margin-bottom "5px"]
                [:#genome 
                 [:ul :list-style-type "none" :padding-left "10px"]
                 [:li link-style]
                 [:#gene-allele [:li :font-size "15px"]]
                 [:#gene-name [:li :font-size "13px"]]
                 [:#gene-mods [:li :font-size "11px"]]]
                [:#vrn-accordion [:div.ui-accordion-content 
                                  :padding-left "10px" :padding-right "10px"]]
                )]
     [:script {:type "text/javascript"
               :src "/static/js/rvar/variation.js"}]
     [:div {:class "span-6 ui-widget ui-widget-content" :id "vrn-pubs"}
      [:h4 {:class "ui-helper-reset ui-widget-header"} "Publication keywords"]
      [:ul
       (for [[kwd _] (take 10 (reverse (sort-by second (get-variant-keywords vrn))))]
         [:li (wikipedia-link (name kwd))])]]
     [:div {:id "vrn-accordion" :class "span-14 last"}
      [:h3 [:a {:href "#"} vrn]]
      [:div 
       [:div {:class "span-9 ui-widget ui-widget-content" :id "genome"}
        [:h4 {:class "ui-helper-reset ui-widget-header"} "Genome"]
        (gene-changes-template vrn)]
       [:div {:class "span-4 last" :id "vrn-links"}
        [:div {:class "ui-widget ui-widget-content" :id "vrn-links-info"}
         [:h4 {:class "ui-helper-reset ui-widget-header"} "More information"]
         [:ul
          (for [link (vrn-links vrn)]
            [:li link])]]
         (let [pro-links (vrn-providers vrn)]
          (if (> (count pro-links) 0)
            [:div {:class "ui-helper-reset ui-widget ui-widget-content"}
              [:h4 {:class "ui-helper-reset ui-widget-header"} "Testing providers"]
              [:ul
               (for [pro-link pro-links]
                 [:li pro-link])]]))]]]
     [:div {:class "span-20 last"}
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

(defn landing-template [request]
  [:div {:id "overview" :class "container span-23 last"}
   [:script {:type "text/javascript" :src "/static/js/rvar/landing.js"}]
   [:style {:type "text/css"}
    (gaka/css [:#statement :font-size "18px" :font-style "italic"]
              [:#highlights :font-size "16px"]
              [:#getting-started :margin-top "50px"])]
   [:div {:id "statement" :class "span-20 last"}
     [:p "Our genomes are a wonderful array of unique variations.
          Access to our own personal sequences gives us the tools
          to explore individual responses to disease and every day life.
          Ourvar provides you with information to make informed decisions
          about genetic sequencing."]]
  [:div {:id "highlights" :class "preprend-top append-bottom"}
    [:div {:class "span-6 colborder"} "Inform yourself about genetic variability and available testing options."]
    [:div {:class "span-6 colborder"} "Share your knowledge of how a variation influences your life and treatment."]
    [:div {:class "span-6 last"} "Discover approaches to improve your health, based on your personal genome."]]
  [:div {:id "getting-started" :class "prepend-top prepend-7 span-6 last"}
    [:a {:href "/health"} "Choose a health topic"]]])

(defn index-template [request]
  "Main r-var display page."
  (let [title "Ourvar: exploring our genomic variability"]
    [:html
     [:head (std-header title)
      [:script {:type "text/javascript"}
       (scriptjure/js (.ready ($ document)
          (fn [] (.tabs ($ "#nav-tabs") {:cookie {:expires 1}})
            (.button ($ "#user-manage a")))))]]
     [:body 
      [:div {:class "container"}
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
         (landing-template request)]]]
     std-footer]]))
