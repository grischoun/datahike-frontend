(ns app.dashboard.ui.queries.datoms
  (:require
   [taoensso.timbre :as log]
   [app.dashboard.mutations.datoms :as dm]
   [app.dashboard.helper :as h]
   [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button b table thead tr th td tbody label textarea]]
   [com.fulcrologic.fulcro.mutations :as m]
   ["material-table" :default MaterialTable]))


(declare Datoms)

;; TODO: Add tests
;; TODO: format code
;; TODO; use Clj-kondo

(defsc QueryInput [this {:query-input/keys [id pull-expr where-expr] :as props}]
  {:query [:query-input/id :query-input/pull-expr :query-input/where-expr]
   :initial-state (fn [_] {:query-input/id  :query-input-init-state
                           :query-input/where-expr "[?e :name 'IVan']"
                           :query-input/pull-expr  "[(pull ?e [*])]"})
   :ident         (fn [] [:query-input/id :the-query-input])}
  (div :.ui.form
    (div :.field
      (label "Pull expression:"
        ;; TODO: try to use this same string preset in the resolver
        (let [set-string! (fn [event]
                            (println "---->" (type event))
                            (m/set-string! this :query-input/pull-expr :event event))]
          (textarea {:value    (or pull-expr "[(pull ?e [*])]")
                     :onChange set-string!
                     :onPaste  set-string!
                     :rows 1}))))
    (div :.field
      (label "Where:"
        (let [set-string! #(m/set-string! this :query-input/where-expr :event %)]
          (textarea {:value (or where-expr "[?e _ _]")
                     :onPaste set-string!
                     :onChange set-string!
                     :rows 2}))))

    #_(div
        (println "Pull: "  pull-expr  "type: " (type pull-expr))
        (println "Where: " where-expr))
    (button :.ui.button
      {:onClick
       (fn []
         (println "after submit: pull-expr " (type pull-expr))
         (println "after submit: Where-Expr: " where-expr)
         (comp/transact! this [(dm/submit-query-input
                                 {:query-input/target-comp :app.dashboard.ui.queries.datoms/Datoms
                                  :query-input/where-expr where-expr ;; "[?e :name \"IVan\"]"
                                  ;; TODO: use the var pull-expr here once we use this same string set in the resolver
                                  :query-input/pull-expr pull-expr})
                               (comp/get-query Datoms)]))}
      "Query!")))

(def ui-query-input (comp/factory QueryInput))




(def mtable (interop/react-factory MaterialTable))

;; TODO: look of QueryInput

(defsc Datoms [this {:datoms/keys [id elements query-input] :as props}]
  {:query [:datoms/id :datoms/elements
           {:datoms/query-input (comp/get-query QueryInput)}]
   :initial-state (fn [_] {:datoms/id      ":datoms-init-state"
                           :datoms/elements {}
                           :datoms/query-input (comp/get-initial-state QueryInput)})
   :ident         (fn [] [:datoms/id :the-datoms])
   :route-segment ["datoms"]}
  ;; TODO: Change so that each collection element below is not inside an array. Use spec to enforce this.
  ;;
  ;; Expects: [[{:id 1, :attribute :age, :value 31, :transac-id 536870961, :added true}]
  ;; [{:id 1, :attribute :name, :value Ivanov, :transac-id 536870961, :added true}]]
  ;;(println "%%%%%%%%% elements: " elements)
  (let [stringified-elems (mapv h/clj-to-str (mapv first elements))
        columns (reduce into (map #(into #{} (keys %)) stringified-elems))]
    (log/info (str "%%%%%%%%% elements-: " stringified-elems))
    (log/info (str "***** Columns: " columns))
    [(div :.ui.two.column.grid
       (div :.row
         (ui-query-input query-input)))
     (div :.row
       (mtable
         {:title    "Datoms"
          :columns  (mapv (fn [c] {:title c :field c}) columns)
          :data     (if (empty? (first stringified-elems))
                      []
                      stringified-elems)

          :editable {:onRowAdd    (fn [newData]
                                    (do
                                      (comp/transact! this
                                        [(dm/update-datoms {:datoms/datom (js->clj newData :keywordize-keys true)
                                                            #_(into [:db/add]
                                                                            (vec (vals (js->clj newData))))
                                                            :datoms/target-comp :app.dashboard.ui.queries.datoms/Datoms})])
                                      (js/Promise.resolve newData)))

                     :onRowUpdate (fn [newData, oldData]
                                    (do
                                      (println "*******" oldData "******" (js->clj newData))
                                      (comp/transact! this
                                        [(dm/update-datoms {:datoms/datom       (js->clj newData ) ;;(vals (js->clj newData))
                                                            :datoms/target-comp :app.dashboard.ui.queries.datoms/Datoms})])
                                      (js/Promise.resolve newData)))
                     :onRowDelete id}}))]))

(def ui-datoms (comp/factory Datoms))




(comment
  (def entity [{:db/id 2 :age 25 :name "Ivan"}])
  (def entities #{[{:db/id 2 :age 25 :name "Ivan"}] [{:db/id 1 :age 44 :name "Petr"}] [{:db/id 3 :player/age 44 :player/event "Petr"}]})

  (def columns (reduce into (map #(into #{} (keys (first %))) entities)))

  columns
  (mapv (fn [c] {:title c :field c}) columns)

  (into #{1 2} (set (keys (first entity))))

  (def res {:db/id 10, :player/event [{:db/id 7} {:db/id 8} {:db/id 11}], :player/name "Paul", :player/team [{:db/id 11} {:db/id 12}]})

  (defn map-vals
    "Maps a function over the values of an associative collection."
    [f m]
    (into {} (map (juxt key (comp f val))) m))

  (defn vec-to-id
    [val]
    (if (vector? val) (str val) val))

  (map-vals vec-to-id res)

  (h/clj-to-str {:db/id :hlle})
  (h/str-to-clj {":db/id" " :hlle" 1 2})
  )
