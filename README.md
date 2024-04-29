# Benchmarking the Utility of $w$-event Differential Privacy Mechanisms -- When Baselines Become Mighty Competitors
This code corresponds to the paper: Schäler, C., Hütter, T., & Schäler, M. (2023). Benchmarking the Utility of w-Event Differential Privacy Mechanisms-When Baselines Become Mighty Competitors. Proceedings of the VLDB Endowment, 16(8), 1830-1842.


## Software requirements

The following list of software is required to execute the experiments:
  * bash
  * python 3
  * curl
  * java jdk
  * java jre
  * docker
  * excel (for ploting)


## Reproduce the experiments

We provide a convenience script to perform the experiments and plot the results shown in Schäler et al. (2023). Note that we assume that all datasets are stored in `./data`. However, due to conflicting licenses, we are not allowed to share the experimental data publicly. The convenience script is executed from the root directory of the repository as follows:
```
sh scripts/perform-experiment.sh
```

Further note that the experiments take approximately 24 hours to complete. The plots can be reproduced by copying the results from `./results` in the excel file in `./figures/vldb-reproducibility.xlsx`.

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
