(ns riemann-console.db)

(def sample-db
  {:loading? false
   :saving? false
   :dashboard-settings? false
   :delete-confirmation? false
   :configuring-widget nil
   :notification {:type :error
                  :message "Connected"}

   :dashboards {"1" "dashboard name"
                "2" "foo board"
                "3" "another board"}

   :streams {"a" []}
   
   :dashboard {:id "1"
               :name "dashboard name"
               :description "dashboard description"
               :endpoint "127.0.0.1:5556"
               :widgets {"a" {:x 0 :y 0 :w 3 :h 6
                              :type :gauge
                              :query "status = \"ok\""}
                         "b" {:x 3 :y 0 :w 3 :h 6
                              :type :gauge
                              :query "status = \"not ok\""}
                         "c" {:x 6 :y 1 :w 6 :h 6
                              :type :time-series
                              :query "some other query"}}}})
