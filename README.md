# Benchmarking the Utility of $w$-event Differential Privacy Mechanisms -- When Baselines Become Mighty Competitors
This code corresponds to the paper: Schäler, C., Hütter, T., & Schäler, M. (2023). Benchmarking the Utility of w-Event Differential Privacy Mechanisms-When Baselines Become Mighty Competitors. Proceedings of the VLDB Endowment, 16(8), 1830-1842.


## Reproduce the experiments

In order to perform the experiments and plot the results by Schaeler et al. (2023), the following steps have to be performed. Unfortunately, we are not able to share the experimental data due to conflicting licenses.
```
sh scripts/perform-experiment.sh
```

### Reproduce the experiments within a Docker container

The `Dockerfile` in the root directory of this repository allows to reproduce the entire experimental evaluation within a Docker a container. To do so, please execute the following commands:
```
mkdir -p results
docker build --no-cache -t streamdp-benchmark .
docker run -d -ti --name streamdp-exp --mount type=bind,source="$(pwd)"/results,target=/usr/src/app/streamdp-benchmark/results streamdp-benchmark
```

This command will persistently store the experimental results in the mounted directory (here, in the created directory `results`). In case that the data should not be persistently stored, remove the `--mount type=bind,source="$(pwd)"/results,target=/usr/src/app/streamdp-benchmark/results` argument.

## Citation:

If you use this code for your work, please cite our paper:
```
@article{schaeler2023dpbench,
  author = {Sch\"{a}ler, Christine and H\"{u}tter, Thomas and Sch\"{a}ler, Martin},
  title = {Benchmarking the Utility of w-Event Differential Privacy Mechanisms - When Baselines Become Mighty Competitors},
  year = {2023},
  issue_date = {April 2023},
  publisher = {VLDB Endowment},
  volume = {16},
  number = {8},
  issn = {2150-8097},
  url = {https://doi.org/10.14778/3594512.3594515},
  doi = {10.14778/3594512.3594515},
  journal = {Proc. VLDB Endow.},
  month = {apr},
  pages = {1830-1842},
  numpages = {13}
}
```

Visit also our project website: https://dbresearch.uni-salzburg.at/projects/dpbench/index.html.
