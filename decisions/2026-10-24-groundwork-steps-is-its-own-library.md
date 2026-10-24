# groundwork-steps is its own library: the steps and the answer's shape the libraries share

**Decision:** a handler's work as steps, and the shape of the answer they end with, are a library of their own, groundwork-steps, one `.cljc` file with no dependencies, which groundwork-bridge, groundwork-access, groundwork-live-updates and groundwork-screens each require instead of one another. It holds:

- the step's shape, `{:name :enter}` (`step-shape`), and `run-steps-until-answered`, `add-answer-to-context`, `does-context-have-answer?`, moved from groundwork-bridge (`groundwork-bridge.steps`), not copied;
- the context's keys, named `:groundwork-steps/…`: `request`, `message-name`, `message-data`, `log`, `shaper`, `answer`, `notes`;
- the refusal, `{:groundwork-steps/refused reason :groundwork-steps/data data :groundwork-steps/request-id id}`, with `make-refusal`, `refuse` and `is-error-answer?`, moved from groundwork-bridge (`groundwork-bridge.answers`);
- `refusals`, the five reasons every answer may carry besides its handler's own, `:groundwork-steps/too-many`, `failed`, `invalid`, `no-answer`, `unfit`, with their data's shapes: groundwork-bridge's own reasons until now, `:groundwork-bridge/…`.

The first release, v0.1.0, was tagged by hand: `tools/bump` releases the next patch of a library's last tag, and a new library has none.

**Why:** groundwork-access's steps read the message's data and log and refuse; groundwork-live-updates puts its notes in the context and makes the browser's no-answer refusal; groundwork-screens tells a refusal from data and a no-answer from the rest. Each needed groundwork-bridge only for these shapes, so each was tied to the whole wire (Jetty, reitit, its settings) and to its releases. With the shapes in a library of their own, the four don't require each other, and the composer (groundwork) is the one place that plugs them together. All five shared reasons move with the refusal: screens reads `no-answer` and `unfit`, live-updates makes `no-answer`, and every app's catalogs hold a text for each, so one namespace for every reason that is no handler's own reads the same everywhere.

**Instead of:**
- the shapes kept in groundwork-bridge, the siblings requiring it: every library above the wire stays tied to it
- each library writing the keys out itself, as plain keywords, without requiring anything: the shape has no home, and a change to it goes wrong unseen in the others
- only `failed` and `no-answer` moving, the other three staying groundwork-bridge's: two namespaces for the reasons that are no handler's own, by which library happens to read them today
- the handler's shape (`:params-shape`, `:answer-shape`, `:refusals`, `:rate-limit`, `:run-steps-in`) moving too: it is the wire's table, read by groundwork-bridge; groundwork-access and groundwork-live-updates read and write its keys as data, as before
