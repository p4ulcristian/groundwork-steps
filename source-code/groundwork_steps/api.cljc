(ns groundwork-steps.api
  "A handler's work as a list of steps over one map, the context, and the
   shape of the answer the steps end with: what the groundwork libraries
   share, so none of them needs another to read or make it. One file for
   both sides.

   A step is {:name a-keyword :enter (fn [context] context)}: it takes the
   context and returns it, changed. The steps run in order, each getting
   what the one before returned, until one adds an answer; the steps after
   it don't run (run-steps-until-answered).

   The context, as the runner of a message's steps (groundwork-bridge)
   puts it together, before the first step:

     {:groundwork-steps/request      the request the message came with (a Ring request), nil for none
      :groundwork-steps/message-name :orders/of-shop
      :groundwork-steps/message-data {:shop/id #uuid \"…\"}
      :groundwork-steps/log          the app's log, with the message's request id as its context
      :groundwork-steps/shaper       the app's shaper}

   A step that learns more (who is asking) puts a log with more context
   back at :groundwork-steps/log, for the steps after it. A step adds the
   answer at :groundwork-steps/answer (add-answer-to-context), or a refusal
   there (refuse). Notes about the answer, besides it, are what the
   context holds at :groundwork-steps/notes when the steps end (the tables
   the answer was read from); they travel beside the answers, under one
   more name:

     {:orders/of-shop […] :groundwork-steps/notes {:orders/of-shop {:tables [\"orders\"]}}}

   A refusal, what an answer is instead of data: a named reason with its
   data, and the request id of the message it answers once it has left:

     {:groundwork-steps/refused    :order/already-shipped
      :groundwork-steps/data       {:order/shipped-at #inst \"…\"}    ; nil for a reason that carries none
      :groundwork-steps/request-id \"5f0c…\"}

   The reason is the key of its text in the app's catalogs, and its data
   the text's arguments: no English travels. refusals are the reasons
   every answer may carry besides a handler's own.")

(def step-shape
  "One step, as a malli shape: {:name a-keyword :enter (fn [context]
   context)}."
  [:map [:name :keyword] [:enter fn?]])

(def refusals
  "The reasons every answer may carry besides its handler's own, each
   with the shape of its data, nil for none. groundwork-bridge gives them:
   on the server the first three, in the browser the last two.
   :groundwork-steps/too-many   over the handler's :rate-limit: {:retry-after-ms n}, how long until one more is let through
   :groundwork-steps/failed     a bug: a step threw, no step answered, an answer or a refusal didn't fit its shape,
                                a reason the handler doesn't declare, a name no handler has; the details are in
                                the log, under the refusal's :groundwork-steps/request-id
   :groundwork-steps/invalid    the params didn't fit the :params-shape: {:errors {:shop/id [\"should be an id\"]}}
   :groundwork-steps/no-answer  no answer came: the network, or a body that isn't the server's JSON (a proxy's
                                502, a 413): {:http-status n}, nil when no response came at all
   :groundwork-steps/unfit      the server's answer didn't fit the browser's shapes, or its name has none:
                                {:errors …}"
  {:groundwork-steps/too-many  [:map {:closed true} [:retry-after-ms :int]]
   :groundwork-steps/failed    nil
   :groundwork-steps/invalid   [:map {:closed true} [:errors :any]]
   :groundwork-steps/no-answer [:map {:closed true} [:http-status [:maybe :int]]]
   :groundwork-steps/unfit     [:map {:closed true} [:errors :any]]})

(defn add-answer-to-context
  "The context with an answer, at :groundwork-steps/answer; no step after
   this one runs."
  [context answer]
  (assoc context :groundwork-steps/answer answer))

(defn does-context-have-answer?
  "Whether a step has added an answer (or a refusal)."
  [context]
  (contains? context :groundwork-steps/answer))

(defn- stop-when-answered
  ;; reduced tells reduce to stop here and return this context
  [context]
  (if (does-context-have-answer? context)
    (reduced context)
    context))

(defn run-steps-until-answered
  "Runs each step's :enter on the context, in order, until one adds an
   answer. Returns the last context."
  [steps context]
  (reduce (fn [context step]
            (stop-when-answered ((:enter step) context)))
          context
          steps))

(defn make-refusal
  "The refusal of a reason with its data (nil for a reason that carries
   none): {:groundwork-steps/refused reason :groundwork-steps/data data}.
   The request id is added as it leaves."
  [reason data]
  {:groundwork-steps/refused reason :groundwork-steps/data data})

(defn refuse
  "The context with a refusal for its answer, a reason with its data (nil
   for a reason that carries none); no step after this one runs.

     (steps/refuse context :order/already-shipped {:order/shipped-at shipped-at})

   Whether the reason is one the handler declares, and the data fits its
   shape, is the runner's to check (groundwork-bridge answers either
   :groundwork-steps/failed)."
  [context reason data]
  (add-answer-to-context context (make-refusal reason data)))

(defn is-error-answer?
  "Whether an answer is a refusal rather than data: a map carrying
   :groundwork-steps/refused. Anything else is data."
  [answer]
  (and (map? answer) (contains? answer :groundwork-steps/refused)))
