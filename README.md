# PachaSketch

Pacha Sketch is a Java framework for efficient approximate aggregation over high-dimensional, mixed-type data. It organizes multidimensional space hierarchically by treating categorical and numerical attributes differently: categorical attributes are represented using AD-tree encodings, while numerical attributes are organized using B-adic hierarchical hypercubes.

Pacha Sketches combine this organization with mergeable base sketches and lightweight indexing structures that prune uninhabited regions before querying. This enables efficient multidimensional aggregate queries over large and sparse datasets while providing configurable trade-offs between accuracy, memory usage, and query performance.

The framework supports equality and membership predicates on categorical attributes and range predicates on numerical attributes, as well as aggregate functions including `COUNT`, `SUM`, `AVG`, `MIN`, and `MAX`. Its modular and mergeable design makes it suitable for distributed, streaming, and federated data processing.

The extended version of the paper, containing additional experiments and results, is available [here](https://github.com/rudip7/PachaSketch/blob/main/pacha_sketch_paper_long_version.pdf).

## Features

- **Mixed-type multidimensional queries:** Supports equality and membership predicates over categorical attributes together with range predicates over numerical attributes.
- **Hierarchical multidimensional organization:** Numerical dimensions are organized using B-adic hierarchical hypercubes, generalizing dyadic range decompositions to multiple dimensions and configurable bases.
- **Categorical data organization:** Categorical attributes are mapped using an AD-tree hierarchy, providing compact identifiers for progressively more general attribute combinations.
- **Sparse-region pruning:** Lightweight indexing structures identify populated regions and prune uninhabited categorical, numerical, and combined regions before querying the underlying sketches.
- **Approximate multidimensional aggregation:** Supports aggregate queries such as `COUNT`, `SUM`, `AVG`, `MIN`, and `MAX` using configurable base sketch implementations.
- **Configurable accuracy and memory:** Users can configure the number of hierarchy levels, B-adic bases, sketch parameters, Bloom-filter false-positive rates, and other construction parameters to trade memory, accuracy, and query performance.
- **Mergeable and modular design:** Pacha Sketches can be constructed from mergeable sketch primitives, enabling composition across distributed or streaming data sources. The sketching and indexing components can also be replaced or extended for different workloads and aggregate functions.
- **Workload-aware optimization:** Supports techniques such as query coarsening and projected subspaces to reduce the number of regions considered during query processing.
- **Experimental framework:** Includes the datasets, query workloads, and experiment implementations used to evaluate Pacha Sketches against state-of-the-art multidimensional sketching approaches.

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven for dependency management

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd PachaSketch
   ```

2. Build the project using Maven:
   ```bash
   mvn clean install
   ```

### Data

The datasets required for running the experiments can be downloaded from the following link:

[Download Datasets](https://tubcloud.tu-berlin.de/s/yW2FRLNtC445nat)

Place the downloaded datasets and specify the path when running the experiments.

### Experiments

All experiments are located in the `pachasketch.experiments` package. Each experiment is implemented as a separate class and can be executed individually or collectively using the `Main` class.

### Usage

1. Prepare your dataset and place it in the appropriate directory.
2. Run the experiments using the `Main` class:
   ```bash
   java -jar target/PachaSketch-<version>-jar-with-dependencies.jar <dataDir> <resultsBaseDir> [flags]
   ```
   Replace `<dataDir>` with the path to your dataset directory and `<resultsBaseDir>` with the directory where results should be stored.

   **Available Flags:**
   - `-a`: Run all experiments.
   - `-u`: Run update efficiency experiments.
   - `-d`: Run different datasets experiments.
   - `-e`: Run error guarantees experiments.
   - `-p`: Run Pacha parameters experiments.
   - `-s`: Run scalability experiments.
   - `-m`: Run memory budget experiments.
   - `-g`: Run different aggregates experiments.
   - `-t`: Run ADTree influence experiments.
   - `-c`: Run max number of cubes experiments.
   - `-x`: Run projected subspaces experiments.
   - `-f`: Run no filters experiments.

   If no flags are provided, all experiments will be executed.

## Project Structure

- `src/main/java`: Contains the main source code for the library.
- `src/test/java`: Contains test cases for the library.
- `src/main/resources/queries`: Directory containing all queries used in the paper.
- `target`: Directory for compiled classes and packaged JAR files.
