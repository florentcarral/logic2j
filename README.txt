   logic2j - Bring Logic to your Java
   ==================================

A library to bring declarative and logic programming to your Java software.

It is designed for first-order predicate formal logic, and includes all necessary
components to manage Terms and their representations, an inference engine solver,
an extensible unification framework, an in-memory or database-backend knowledge base.

This work was inspired by "tuprolog" from the University of Bologna, Italy. 
This is a major rewrite with completely different unification and inference algorithms.

The design guidelines were: close bidirectionnal integration to any style of Java, minimal dependencies, fabulous features, small footprint, and high performance.
The driver was to implement a rule engine that "reasons" against large data sets, not only objects in the VM.

Although close to Prolog, this is NOT a Prolog environment, but would be a good candidate to build one...

More documentation in the Wiki to come.

You must have received a LICENSE.txt file with this software package.
