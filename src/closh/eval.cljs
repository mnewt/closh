(ns closh.eval
  (:require [lumo.repl]
            [cljs.tools.reader]
            [closh.reader]
            [goog.object :as gobj]))

;; Make lumo's print a noop since we process resulting value ourselves
(gobj/set js/$$LUMO_GLOBALS "doPrint" identity)

(def handle-error-orig lumo.repl/handle-error)

(defn handle-error [error stacktrace?]
  (if (= (.-message (ex-cause error)) "Script execution interrupted.")
    (js/console.log " Interrupted")
    (handle-error-orig error stacktrace?)))

(defn execute-text
  "Evals a string via lumo."
  [source]
  ;; Execute does not return value but binds it to *1
  (lumo.repl/execute-text source {:expression? true})
  *1)

(defn execute-command-text
  "Evals a string in command mode via lumo."
  ([source]
   (execute-command-text source closh.reader/read-sh))
  ([source reader-fn]
   ;; Execute does not return value but binds it to *1
   (with-redefs [cljs.tools.reader/read reader-fn
                 lumo.repl/handle-error handle-error]
     (lumo.repl/execute-text source {:expression? true}))
   *1))

(execute-text
  (pr-str
    '(do
       (require '[lumo.io :refer [slurp spit]]
                '[closh.core :refer [shx expand expand-partial expand-command expand-redirect pipe pipe-multi pipe-map pipe-filter process-output wait-for-process wait-for-pipeline pipeline-condition process-value]]
                '[closh.builtin :refer [cd exit quit getenv setenv]]
                '[closh.util :refer [source-shell]]
                '[clojure.string :as st])
       (require-macros '[closh.core :refer [sh sh-str sh-code sh-ok sh-seq sh-lines sh-value]])

       (defn closh-prompt []
         "$ ")

       (defn closh-title []
         (str "closh " (js/process.cwd)))

       ;; Return nil otherwise #'cljs.user/closh-prompt got printed every time exception was thrown
       nil)))
