## Hospitality-House-Date-Analyzer

### CSVSearchClient
- Searches services dates in csv input file against program entry/exit date range csv input file.
- Output file reflects whether or not client's service date lies within a corresponding program date range (pass/fail).

### CSVSearchClientOverlap
- Analyzes program entry/exit date ranges to establish overlaps.
- Provides extensive feedback on overlaps, including which rows overlap, how they overlap(overlap type), and exact dates for when overlaps start and end.
- Multiple overlaps occuring with a single row are displayed in output file as "copy" rows in order to establish all overlaps within a single program run.
