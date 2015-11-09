#!/usr/bin/env perl

## Tiny tool to print out all cluster jobs of the current user with Job_ID, Job_Status and the FULL Job_Name
## Can be used e.g. to delete all jobs for a certain PID:
## qname.pl | grep KILLME | cut -d' ' -f1 | xargs qdel
##
## Matthias Schlesner, 2012-08-14

use strict;
use warnings;
use v5.10;

my $s = `qstat -x`;
#say $s;
my $user = `whoami`;
chomp $user;
#say $user;
my @jobs = grep {/$user/} split(/\<\/Job\>/, $s);
my ($id, $name, $state);
foreach my $job (@jobs) {
  ($id, $name, $state) = $job =~ /<Job_Id>([^.]+).+<Job_Name>([^<]+).+<job_state>([^<]+)/;
  printf("%-10s%s %s\n", $id, $state, $name);
}
