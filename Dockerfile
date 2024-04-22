# syntax=docker/dockerfile:1

# The MIT License (MIT)
# Copyright (c) 2024 Thomas Huetter.
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# Description: Experimental evaluation performed by Schaeler et al. (2023) in
# "Benchmarking the Utility of w-Event Differential Privacy Mechanisms - When 
# Baselines Become Mighty Competitors".

# Execution: First, create a directory 'results' where the container
# persistently stores the results. Next, build the image. Finally, run the
# experiments in the docker container. To do so, execute the following
# instructions:
#  - Prepare: mkdir results
#  - Build: docker build --no-cache -t streamdp-benchmark .
#  - Run: docker run -ti --name dpbench-exp --mount \
#         type=bind,source="$(pwd)"/results, \
#         target=/usr/src/app/streamdp-benchmark/results streamdp-benchmark

FROM debian:10

# Set working directory
WORKDIR /usr/src/app

# LABEL about the custom image
LABEL maintainer="thomas.huetter@plus.ac.at"
LABEL version="1.0"
LABEL description="This is custom Docker Image for the reproducibility of the \
experimental evaluation published in the original paper by Schaeler et al. at \
VLDB 2023."

# Disable Prompt During Packages Installation
ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt update

# Install python 3.7
RUN apt install software-properties-common -y
RUN add-apt-repository ppa:deadsnakes/ppa
RUN apt install python3.7 -y

# Add 3.7 to the available alternatives
RUN update-alternatives --install /usr/bin/python python /usr/bin/python3.7 1

# Set python3.7 as the default python
RUN update-alternatives --set python /usr/bin/python3.7

# Install pip
RUN apt install python3-pip -y
RUN python -m pip install --upgrade pip

# Install git
RUN apt install gcc git git-lfs -y

# Install java
RUN apt install default-jre -y

# Clean apt
RUN apt clean

# Install python libraries using pip (including TimeEval)
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir TimeEval==1.2.4

# Clone repository with experimental framework
RUN git clone https://github.com/chryenix/streamdp-benchmark.git
WORKDIR ./streamdp-benchmark

# Setup experiment for anomaly detection.
RUN chmod 0755 scripts/perform-experiment.sh
CMD scripts/perform-experiment.sh
