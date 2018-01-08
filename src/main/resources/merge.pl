#!/usr/bin/perl

use strict;
use Time::Local;

my @files = @ARGV;

print "(?<host>[^|]+)\\|(?<clock>[^|]+)\\|(?<event>.*)\\n\n";
print "====\n";

foreach my $file (@files) {
  open(FILE, '<', $file) or die "Could not open '$file': $!\n";
  while (<FILE> =~ /(.*?32m)(.*?)(.\[0;39m .*)/) {
    my $line = "$1$2$3";
    my ($hour,$min,$sec,$msec) = $2.split(/[:.]/);
    my $time = timelocal($sec,$min,$hour,1,0,2018);
    print("$file|{\"$file\": $time}|$1$2$3\n");
  }
  close(FILE);
}
