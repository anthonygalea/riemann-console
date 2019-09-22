(ns riemann-console.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [riemann-console.events :as events]
   [riemann-console.subs :as subs]
   ["echarts" :as echarts]
   ["react-grid-layout" :refer (WidthProvider Responsive)]
   ["react-select" :default react-select]))

;; utils

(defn button [props children]
  [:a.link.dim.br1.ba.pa2.near-white.pointer.b--black.bg-dark-gray
   props
   children])

(defn panel-button [props children]
  [:a.link.dim.br1.pointer.fr.near-white
   props
   children])

;; navbar

(defn endpoint-text-field []
  (let [endpoint (re-frame/subscribe [::subs/dashboard-endpoint])]
    [:span
     [:input.input-reset.ba.dib.pa2.bg-dark-gray.near-white.b--black.br1.w-50.mr2
       {:type "text"
        :value @endpoint
        :on-change #(re-frame/dispatch [::events/dashboard-endpoint-changed
                                        (-> % .-target .-value)])}]
     [:span.bg-dark-gray.pa2.mr2.ba.b--black
      (let [streams? (re-frame/subscribe [::subs/streams?])]
        (if @streams?
          [:i.fas.fa-satellite-dish.green]
          [:i.fas.fa-satellite-dish.red]))]]))

(defn add-widget-button []
  [button
   {:class "bg-dark-green mr2"
    :on-click #(re-frame/dispatch [::events/widget-added])}
   [:i.fas.fa-chart-pie]])

(defn save-dashboard-button []
  (let [saving? @(re-frame/subscribe [::subs/saving?])]
    [button
     {:on-click #(re-frame/dispatch [::events/dashboard-saved])}
     (if saving?
       [:i.fas.fa-spinner]
       [:i.fas.fa-save])]))

(defn dashboard-settings-button []
  [button
   {:class "mr2"
    :on-click #(re-frame/dispatch [::events/dashboard-settings?])}
   [:i.fas.fa-cog]])

(defn dashboard-controls []
  [:div.tr-l.tc
   [add-widget-button]
   [endpoint-text-field]
   [dashboard-settings-button]
   [save-dashboard-button]])

(defn logo []
  (let [loading? @(re-frame/subscribe [::subs/loading?])]
    [:div
     (if loading?
       [:i.fas.fa-border-none.fa-2x.pl2.pr3.v-mid.gray]
       [:a {:href "/"}
        [:i.fas.fa-border-all.fa-2x.ph2.gray]])]))

(defn add-dashboard-button []
  [button
   {:class "bg-dark-green mr2"
    :on-click #(re-frame/dispatch [::events/dashboard-added])}
   [:i.fas.fa-plus]])

(defn copy-dashboard-button []
  [button
   {:class "mr2"
    :on-click #(re-frame/dispatch [::events/dashboard-copied])}
   [:i.fas.fa-copy]])

(defn dashboard->option [[uuid name]]
  {:value uuid :label name})

(defn dashboard-selector []
  (let [dashboards (re-frame/subscribe [::subs/dashboards])
        current-dashboard (re-frame/subscribe [::subs/current-dashboard])]
    [:div.mr2.w5
     [:> react-select
      {:classNamePrefix "selector"
       :options (->> @dashboards (sort-by val) (map dashboard->option) clj->js)
       :noOptionsMessage (fn [_] "No dashboards")
       :value (clj->js {:value @current-dashboard
                        :label (get @dashboards @current-dashboard)})
       :on-change #(re-frame/dispatch [::events/load-dashboard
                                       (:value (js->clj % :keywordize-keys true))])}]]))

(defn notifier []
  (when-let [notification @(re-frame/subscribe [::subs/notification])]
    [:div.f6.pa2.tc.mv1
     (case (:type notification)
       :success
        [:span.bg-dark-green.pa2.br1.b--black
         [:i.fas.fa-info-circle.mr2]
         (:message notification)]
       :error
        [:span.bg-dark-red.pa2.br1.b--black
         [:i.fas.fa-exclamation-circle.mr2]
         (:message notification)])]))

(defn dashboard-selector-controls []
  [:div.flex.items-center.tl-l.tc
   [logo]
   [dashboard-selector]
   [copy-dashboard-button]
   [add-dashboard-button]])

(defn navbar
  []
  [:header.fixed.w-100.pv2.pv2-ns.ph3-m.ph2-l.z-1.bg-near-black
   [:nav.w-100.flex.flex-wrap.near-white.f4.fw6.justify-between-l.justify-around
    [dashboard-selector-controls]
    [notifier]
    [dashboard-controls]]])

;; dashboard settings

(defn dashboard-settings-delete-button []
  [button
   {:class "bg-dark-red"
    :on-click #(re-frame/dispatch [::events/delete-confirmation])}
   [:span
    [:i.fas.fa-trash-alt.mr2]
    "Delete dashboard"]])

(defn dashboard-settings-cancel-button []
  [button
   {:class "mr2"
    :on-click #(re-frame/dispatch [::events/dashboard-settings?])}
   "Cancel"])

(defn dashboard-settings-save-button []
  [button
   {:class "bg-dark-green"
    :on-click #(re-frame/dispatch [::events/dashboard-settings-saved])}
   "Save"])

(defn dashboard-settings-close-button []
  [panel-button
   {:on-click #(re-frame/dispatch [::events/dashboard-settings?])}
   [:i.fas.fa-times]])

(defn dashboard-settings []
  (let [dashboard-settings? (re-frame/subscribe [::subs/dashboard-settings?])
        dashboard-name (re-frame/subscribe [::subs/dashboard-name])
        dashboard-description (re-frame/subscribe [::subs/dashboard-description])]
    (when @dashboard-settings?
      [:div.fixed.top-0.bottom-0.right-0.left-0.bg-black-90.near-white.pa3.z-1
       [:div.mw6.bg-near-black.near-white.center
        [:div.pa2
         [:h4.dib.near-white.ma0 "Dashboard Settings"]
         [dashboard-settings-close-button]
         [:form.ph0.w-100
          [:fieldset.ba.b--transparent.ph0.mh0
           [:div.mt3
            [:label.db.fw6.lh-copy.f6 {:for "name"} "Name"]
            [:input.pa2.input-reset.ba.bg-dark-gray.w-100.near-white.b--black
             {:name "name" :type "text" :defaultValue @dashboard-name
              :on-change #(re-frame/dispatch [::events/dashboard-name-changed
                                              (-> % .-target .-value)])}]]
           [:div.mv3
            [:label.db.fw6.lh-copy.f6 {:for "description"} "Description"]
            [:textarea.pa2.input-reset.bg-dark-gray.ba.w-100.near-white.b--black
             {:name "description" :rows 7 :defaultValue @dashboard-description
              :on-change #(re-frame/dispatch [::events/dashboard-description-changed
                                              (-> % .-target .-value)])}]]
           [dashboard-settings-delete-button]
           [:div.tr.pt4
            [dashboard-settings-cancel-button]
            [dashboard-settings-save-button]]]]]]])))

;; delete dashboard confirmation

(defn delete-confirmation []
  (when-let [delete-confirmation? @(re-frame/subscribe [::subs/delete-confirmation?])]
    [:div.fixed.top-0.bottom-0.right-0.left-0.bg-black-90.near-white.pa3.z-1
     [:div.mw6.bg-near-black.near-white.center
      [:div.pa2.tc
       [:form.measure.center
        [:fieldset.ba.b--transparent.ph0.mh0
         [:legend.f4.fw6 "Are you sure you want to delete this dashboard?"]
         [:div.pt3
          [button
           {:class "mr2"
            :on-click #(re-frame/dispatch [::events/dashboard-deleted])}
           "Yes"]
          [button
           {:on-click #(re-frame/dispatch [::events/delete-confirmation])}
           "No"]]]]]]]))

;; widget-configurer

(defn widget-configurer-close-button []
  [panel-button
   {:on-click #(re-frame/dispatch [::events/widget-configurer-closed])}
   [:i.fas.fa-times]])

(defn widget-configurer-title [widget-id title]
  [:div
   [:label.db.fw6.lh-copy.f6.tl "Title"]
   [:input.pa2.input-reset.ba.w-100.bg-dark-gray.near-white.b--black.br1
    {:name "title"
     :type "text"
     :value title
     :on-change #(re-frame/dispatch [::events/widget-title-changed widget-id
                                     (-> % .-target .-value)])}]])

(defn widget-configurer-query [widget-id query]
  [:div.mt2
   [:label.db.fw6.lh-copy.f6 {:for "query"} "Query"]
   [:textarea.pa2.input-reset.bg-dark-gray.ba.w-100.near-white.b--black.br1
    {:name "query" :rows 8 :defaultValue query
     :placeholder "The query to send to riemann"
     :on-change #(re-frame/dispatch [::events/widget-query-changed widget-id
                                     (-> % .-target .-value)])}]])

(defn widget-configurer-max-events [widget-id max-events]
  [:div.mt2
   [:label.db.fw6.lh-copy.f6.tl "Max events"]
   [:input.pa2.input-reset.ba.w-100.bg-dark-gray.near-white.b--black.br1
    {:name "max-events"
     :type "number"
     :min 1
     :value max-events
     :on-change #(re-frame/dispatch [::events/widget-max-events-changed widget-id
                                     (-> % .-target .-value int)])}]])

(defn widget-configurer-type-selector [widget-id type]
  [:div
   [:label.db.fw6.lh-copy.f6.tl "Type"]
   [:select.pa2.input-reset.ba.w-100.bg-dark-gray.near-white.b--black.br1
    {:name "type"
     :value (name type)
     :on-change #(re-frame/dispatch [::events/widget-type-changed widget-id
                                     (-> % .-target .-value)])}
    [:option {:label "Gauge"
              :value "gauge"}]
    [:option {:label "Table"
              :value "table"}]
    [:option {:label "Time Series"
              :value "time-series"}]]])

(defn widget-configurer-show-legend [widget-id show-legend]
  [:label.db.fw6.lh-copy.f6.tl.pointer.mt2
   [:input.mr2
    {:type "checkbox"
     :defaultChecked show-legend
     :value show-legend
     :on-click #(re-frame/dispatch [::events/widget-show-legend widget-id])}]
   "Show legend"])

(defn widget-configurer-min [widget-id min]
  [:div.mt2
   [:label.db.fw6.lh-copy.f6.tl "Min"]
   [:input.pa2.input-reset.ba.w-100.bg-dark-gray.near-white.b--black.br1
    {:name "min"
     :type "number"
     :value min
     :on-change #(re-frame/dispatch [::events/widget-min-changed widget-id
                                     (-> % .-target .-value int)])}]])

(defn widget-configurer-max [widget-id max]
  [:div.mt2
   [:label.db.fw6.lh-copy.f6.tl "Max"]
   [:input.pa2.input-reset.ba.w-100.bg-dark-gray.near-white.b--black.br1
    {:name "max"
     :type "number"
     :value max
     :on-change #(re-frame/dispatch [::events/widget-max-changed widget-id
                                     (-> % .-target .-value int)])}]])

(defn widget-configurer-fields [widget-id fields]
  [:div.mt2
   [:label.db.fw6.lh-copy.f6.tl "Fields"]
   [:input.pa2.input-reset.ba.w-100.bg-dark-gray.near-white.b--black.br1
    {:name "fields"
     :type "text"
     :defaultValue fields
     :on-blur #(re-frame/dispatch [::events/widget-fields-changed widget-id
                                   (-> % .-target .-value)])}]])

(defn widget-configurer []
  (let [configuring-widget (re-frame/subscribe [::subs/configuring-widget])
        widget (re-frame/subscribe [::subs/dashboard-widget @configuring-widget])]
    (when @configuring-widget
      [:div.dib.fixed.bottom-0.bg-black-90.near-white.pa3.z-1.right-0.left-0
        [:div.bg-near-black.pa2.mw8.center
         [:h4.dib.near-white.ma0 "Configure"]
         [widget-configurer-close-button]
         [:div.flex.flex-wrap.justify-between-l.justify-around.pv2
          [:div.w-50-l.w-100.pr2
           [widget-configurer-title @configuring-widget (:title @widget)]
           [widget-configurer-max-events @configuring-widget (:max-events @widget)]
           [widget-configurer-query @configuring-widget (:query @widget)]]
          [:div.w-50-l.w-100
           [widget-configurer-type-selector @configuring-widget (:type @widget)]
           (case (:type @widget)
            :time-series
            [widget-configurer-show-legend @configuring-widget (:show-legend @widget)]
            :gauge
            [:div
             [widget-configurer-min @configuring-widget (:min @widget)]
             [widget-configurer-max @configuring-widget (:max @widget)]]
            :table
            [widget-configurer-fields @configuring-widget (:fields @widget)]
            [:span])]]]])))

;; charts

(defn echart-render []
  [:div {:style {:width "100%" :height "90%"}}])

(defn echart [option]
  (let [chart (atom nil)]
    (reagent/create-class
     {:reagent-render echart-render
      :component-did-mount #(let [c (echarts/init (reagent/dom-node %) "dark")]
                              (reset! chart c)
                              (.setOption c (clj->js option)))
      :component-did-update #(let [o (first (rest (reagent/argv %)))]
                               (.setOption @chart (clj->js o))
                               (.resize @chart))})))


(defn event->tr [fields idx event]
  [:tr {:key idx}
   (->> (select-keys event fields)
        (map #(vector :td.pv2.bb.b--black-20 {:key (str idx (key %))} (val %))))])

(defn table [widget-id]
  (let [filter-value (reagent/atom nil)
        sort (reagent/atom {:sort-val nil :ascending true})
        update-sort-value (fn [new-val]
                            (if (= new-val (:sort-val @sort))
                              (swap! sort update-in [:ascending] not)
                              (swap! sort assoc :ascending true))
                            (swap! sort assoc :sort-val new-val))
        filter-content (fn [content]
                         (if (string/blank? @filter-value)
                           content
                           (filter #(re-find (->> (str @filter-value)
                                                  (string/upper-case)
                                                  (re-pattern))
                                             (string/upper-case (str (vals %))))
                                   content)))
        sort-content (fn [content]
                       (if (:sort-val @sort)
                         (sort-by #(get % (:sort-val @sort))
                                  #(if (:ascending @sort)
                                     (compare %1 %2)
                                     (compare %2 %1))
                                  content)
                         content))]
    (fn [widget-id]
      [:div
       [:div.dib.fr
        [:i.fas.fa-search.mr2.black-50]
        [:input.input-reset.ba.br1.pa2.bg-dark-gray.near-white.b--black
         {:type "text" :class "mv3 mr1 b--black-40" :value @filter-value
          :on-mouse-down #(.stopPropagation %)
          :on-change #(reset! filter-value (-> % .-target .-value))}]]
       (let [stream (re-frame/subscribe [::subs/stream widget-id])
             fields (re-frame/subscribe [::subs/dashboard-widget-fields widget-id])]
         [:table.f6.w-100.mt2.near-white.collapse
          [:thead
           [:tr
            (doall
             (for [field @fields]
               [:th.bb.b--black-20.tl.pb3.pointer
                {:key field
                 :on-click #(update-sort-value field)}
                field
                [:i.fas.ml1
                 {:class (cond
                           (not= field (:sort-val @sort)) "fa-sort gray"
                           (:ascending @sort) "fa-sort-up"
                           :else "fa-sort-down")}]]))]]
          [:tbody.lh-copy
           (when @stream
             (->> @stream
                  (filter-content)
                  (sort-content)
                  (map-indexed (partial event->tr @fields))))]])])))

(defn gauge [{:keys [id min max w h] :as widget}]
  (let [stream (re-frame/subscribe [::subs/stream id])]
    [echart {:series [{:type "gauge"
                       :size [w h]
                       :center ["50%" "60%"]
                       :splitNumber 1
                       :startAngle 190
                       :endAngle -10
                       :axisLine {:lineStyle {:color [[0.8 "#19A974"] [1 "#FF4136"]]
                                              :width 10}}
                       :axisLabel {:textStyle {:color "#fff"}}
                       :axisTick {:length 0}
                       :splitLine {:length 10}
                       :min min
                       :max max
                       :data [{:value (-> @stream last (get "metric"))}]}]}]))

(defn service->echart-series [[service data]]
  {:name service
   :type "line"
   :data (map #(vector (get % "time") (get % "metric")) data)})

(defn time-series [{:keys [id w h show-legend] :as widget}]
  (let [stream (re-frame/subscribe [::subs/stream id])]
    [echart {:size [w h]
             :legend (if show-legend
                       {:show true
                        :top "30"
                        :left "20"
                        :right "30"
                        :data (->> @stream
                                   (group-by #(get % "service"))
                                   (keys))}
                       {:show false})
             :tooltip {:trigger "axis"
                       :backgroundColor "#111"
                       :axisPointer {:type :cross
                                     :animation false
                                     :label {:backgroundColor "#111"}}}
             :grid {:containLabel true
                    :left "20"
                    :right "30"
                    :bottom "0"
                    :height (if show-legend "65%" "90%")}
             :animation false
             :xAxis {:type "time"
                     :splitLine {:show true}}
             :yAxis   {:type "value"}
             :series (->> @stream
                          (map #(select-keys % ["service" "time" "metric"]))
                          (group-by #(get % "service"))
                          (map service->echart-series))}]))

;; widget

(defn configure-widget-button [widget-id]
  [panel-button
   {:class "mr2"
    :on-click #(re-frame/dispatch [::events/configure-widget widget-id])}
   [:i.fas.fa-cog]])

(defn close-widget-button [widget-id]
  [panel-button
   {:on-click #(re-frame/dispatch [::events/widget-deleted widget-id])}
   [:i.fas.fa-times]])

(defn widget-panel [{:keys [id type title x y w h] :as widget}]
  (let [configuring-widget (re-frame/subscribe [::subs/configuring-widget])]
    [:div.pa2.br1.bg-dark-gray
     {:key id
      :data-grid {:x x :y y :w w :h h}
      :class (if (= id @configuring-widget) "ba bw1 b--gray" "")}
     [:h4.dib.near-white.ma0 title]
     [close-widget-button id]
     [configure-widget-button id]
     [:div.overflow-y-scroll
      {:style {:height "95%"}}
      (case type
        :gauge [gauge widget]
        :table [table id]
        :time-series [time-series widget])]]))

;; dashboard

(def grid (reagent/adapt-react-class (WidthProvider. Responsive)))

(defn dashboard []
  (when-let [widgets @(re-frame/subscribe [::subs/dashboard-widgets])]
    [:div {:style {:padding-top "3rem"}}
     [grid {:breakpoints {:lg 1200 :md 996 :sm 768 :xs 480 :xxs 0}
            :cols {:lg 24 :md 20 :sm 12 :xs 8 :xxs 4}
            :rowHeight 25 :width 1200
            :onLayoutChange #(re-frame/dispatch
                              [::events/layout-changed (js->clj % :keywordize-keys true)])}
      (doall (for [[id widget] widgets]
               (widget-panel (assoc widget :id id))))]]))

(defn app []
  [:div.sans-serif
   [navbar]
   [dashboard]
   [dashboard-settings]
   [delete-confirmation]
   [widget-configurer]
   [:script {:src "js/dark.js" :type "text/javascript"}]])
 
