(ns groundwork-steps.api-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [groundwork-steps.api :as steps]))

(defn- add-to-trail [step-name]
  {:name  step-name
   :enter (fn [context] (update context :trail conj step-name))})

(deftest steps-run-in-order-until-answered
  (testing "every step runs, in order, each on what the one before returned"
    (is (= [:a :b :c]
           (:trail (steps/run-steps-until-answered
                    [(add-to-trail :a) (add-to-trail :b) (add-to-trail :c)]
                    {:trail []})))))
  (testing "an answer stops the steps after it"
    (let [context (steps/run-steps-until-answered
                   [(add-to-trail :a)
                    {:name :answer :enter #(steps/add-answer-to-context % :done)}
                    (add-to-trail :c)]
                   {:trail []})]
      (is (= [:a] (:trail context)))
      (is (steps/does-context-have-answer? context))
      (is (= :done (:groundwork-steps/answer context)))))
  (testing "an answer of nil is an answer"
    (is (steps/does-context-have-answer? (steps/add-answer-to-context {} nil))))
  (testing "no steps: the context comes back as it was"
    (is (= {:trail []} (steps/run-steps-until-answered [] {:trail []})))))

(deftest a-refusal-is-a-named-reason-with-its-data
  (testing "refuse answers with the refusal, and stops the steps after it"
    (let [context (steps/run-steps-until-answered
                   [{:name :refuse :enter #(steps/refuse % :order/already-shipped {:order/shipped-at "today"})}
                    (add-to-trail :after)]
                   {:trail []})]
      (is (= {:groundwork-steps/refused :order/already-shipped :groundwork-steps/data {:order/shipped-at "today"}}
             (:groundwork-steps/answer context)))
      (is (= [] (:trail context)))))
  (testing "make-refusal: the same map, nil for a reason that carries no data"
    (is (= {:groundwork-steps/refused :shop/closed :groundwork-steps/data nil} (steps/make-refusal :shop/closed nil))))
  (testing "is-error-answer?: a map carrying :groundwork-steps/refused, nothing else"
    (is (true? (steps/is-error-answer? (steps/make-refusal :shop/closed nil))))
    (is (true? (steps/is-error-answer? {:groundwork-steps/refused :groundwork-steps/no-answer :groundwork-steps/data {:http-status nil}})))
    (is (false? (steps/is-error-answer? {:error "Not found."})))
    (is (false? (steps/is-error-answer? [:groundwork-steps/refused])))
    (is (false? (steps/is-error-answer? nil)))))

(deftest the-shared-reasons
  (is (= #{:groundwork-steps/too-many :groundwork-steps/failed :groundwork-steps/invalid
           :groundwork-steps/no-answer :groundwork-steps/unfit}
         (set (keys steps/refusals))))
  (is (nil? (:groundwork-steps/failed steps/refusals)) "a bug carries no data"))
