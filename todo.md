everything

Pipelines:

* Job Creation:
  * JobController -> JobService -> Manager -> Task -> JobExecutionService -> JobRepository
* Job Status Fetching:
  * JobController -> JobService -> JobRepository
    

Create predetermined task types
Create dummy workers for each type