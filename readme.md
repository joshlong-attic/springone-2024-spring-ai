# Bootiful Spring AI 
by Dr. Mark Pollack, Dr. Rod Johnson, and Josh

you can find [Rod's code here](https://github.com/johnsonr/instrumented-rag)

https://chat.lmsys.org

* "hi, Spring fans!"
* DJ (~5 mins): officiate the project
  * JVM devs are important to the future of AI.
  * people are not going to switch to Python.
  * Adding GenAI to existing applications with Spring AI is trivial. Want to add functions to your AI flow, woohoo! You've got a lot of valuable code in Spring already, and it's trivial to connect the model to your APIs.
  * you should be excited about this!
  * requires a different skill set to make apps useful than model builders and data scientists
  * Spring's been around for a really, really long time.
  * Portable Service Abstractions, AOP, DI, autoconfiguration
* DP (~5 mins): 
  * history of AI
  * problems/solutions 
  * constraints inform architecture (microservices vs. team size, models vs. speed/intelligence)
  * choose your model wisely (costs vs. speed/accuracy) "which one should we choose?"
  * "vibe checks are all you need."
  * https://chat.lmsys.org
* JL/DP:
  * start.spring.io: `web`, `graalvm`, `jdbc`, `openai`, `pgvector`, `actuator`, `zipkin`, `docker compose`
  * setup  :
    * API key
    * remove _Docker Compose_ from `pom.xml`, specify `spring.datasource.*`, export the ports in `compose.yml`, `docker compose up -d`
    * model
    * _Spring AI M2_!
* DP/JL: introduce the `ChatClient` with a simple joke controller
* DP/JL: structured output (`Joke.class`)
* DP/JL: RAG
* DP/JL: Chat Memory
* DP/JL: functions (show getting the status of a purchase (a `Transaction`))
* DP/JL: observable AI: observability
* DP/JL: efficient AI: virtual threads and GraalVM
