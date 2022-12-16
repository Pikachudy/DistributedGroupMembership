#!/bin/bash

keyword=$1
path=$2

grep -r -B 1 "$keyword" "$path"
