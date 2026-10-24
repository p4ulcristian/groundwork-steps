(ns groundwork-steps.api-names-test
  "The public names of groundwork-steps's api. Adding or removing one fails
   here, so the api changes on purpose."
  (:require
   [clojure.test :refer [deftest is]]
   [groundwork-steps.api]))

(deftest the-api-names
  (is (= '#{step-shape refusals
           run-steps-until-answered add-answer-to-context does-context-have-answer?
           make-refusal refuse is-error-answer?}
         (set (keys (ns-publics 'groundwork-steps.api))))))
