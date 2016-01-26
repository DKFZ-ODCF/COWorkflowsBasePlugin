Initial import for the COWorkflows (Computational Oncology Workflows Base Plugin for Roddy) repo into Phabricator.

* Version update to 1.1.20

- Reverted references to FileSystemAccessProvider back to FileSystemInfoProvider to ensure compatibility of current COWorkflows to older version of Roddy (e.g. 2.2.66 currently used in OTP).
- Changed COProjectsRuntimeService so, that a library name can be set when getting the sample or sequence directory.
- Function added to get the read groups of a bam file from the bam header. Small fix in the SQ count
- Add readme file.

