everything

Pipelines:

* Job Creation:
  * JobController -> JobService -> Manager -> Task -> JobExecutionService -> JobRepository
* Job Status Fetching:
  * JobController -> JobService -> JobRepository
    

Need proper error handling, not just throws. Especially for user response. 


Tasks? 
1. FETCH_URL             → fetch URL, return status and size
2. HASH_TEXT             → hash string (SHA-256)
3. DOWNLOAD_FILE         → download file to disk
4. PROCESS_FILE          → read file and compute stats (lines, size)
5. COUNT_WORDS           → count words in text
6. ADD_NUMBERS           → a simple arithmetic baseline
7. TRANSFORM_JSON        → reshape JSON structure
8. VALIDATE_JSON         → validate against schema/rules
9. EXTRACT_METADATA      → file metadata (size, type, timestamps)
10. COMPRESS_FILE        → zip/gzip a file
11. DECOMPRESS_FILE      → unzip file
12. GENERATE_THUMBNAIL   → image/video preview generation
13. CONVERT_FORMAT       → CSV ↔ JSON, txt ↔ csv
14. CHECK_HEALTH         → ping service / endpoint
15. PING_HOST            → network latency check
16. BATCH_PROCESS        → run operation over many items
17. CLEANUP_OLD_DATA     → delete old files/records
18. REINDEX_DATA         → rebuild indexes (simulated)
19. SORT_NUMBERS         → sort large dataset
20. GENERATE_RANDOM_DATA → create test dataset
21. ENRICH_DATA          → add computed fields to payload
22. FILTER_DATA          → filter dataset by rules
23. AGGREGATE_DATA       → compute sums/averages
24. EXTRACT_AUDIO        → pull audio from video file
25. TRANSCODE_VIDEO      → simulate/perform format conversion