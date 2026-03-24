everything

Pipelines:

* Job Creation:
  * JobController -> JobService -> Manager -> Task -> JobExecutionService -> JobRepository
* Job Status Fetching:
  * JobController -> JobService -> JobRepository
    

Need proper error handling, not just throws. Especially for user response. 