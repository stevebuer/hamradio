#!/bin/bash

# SBAprs: generate SVG graph file

wget localhost:8000/graph -O g.dot

dot -Tsvg -O g.dot

open g.dot.svg
