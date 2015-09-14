#Topic Model Algorithms
The Java package TopicModelAlgorithms is released to provide alternatives for topic models implementaions using Java language. Participation, bug reports, comments and suggestions about TopicModelAlgorithms are highly appreciated.

##Latent Dirichlet Allocation (LDA) [1]
One of the popular topic models has recently emerged as the method of choice for working with large collections of text documents. It has recently begun to unfold as the subject of choice for working with large collections of text documents. 
 
##Sentence Latent Dirichlet Allocation (SLDA) [2]
A probabilistic generative model that assumes all words in a single sentence are generated from one aspect (topic). This is the basic difference from the general LDA model [REF], which is based on each word in the document generated from one topic.
 
##Guided Latent Dirichlet Allocation (GLDA) [3]
It is like in LDA, each document is assumed to be a mixture over topics but each topic is a convex combination of a seed topic and a traditional LDA style topic. GLDA guide the model to learn a desired topic by providing seed words in each topic. 

##Special Words topic model (SW) [4]
It is based on the assumption that a word can either be generated from a document-specific distribution, or generated via the topic route. SW has a similar general structure to the LDA model but with additional machinery to handle special words, there is a multinomial variable x associated with each word that is over the two different “sources” of words. When x = 0, the document-specific distribution generates the word, and when x = 1, one of the topic distributions generates the word.

#References
#######[1]- Blei, David M.; Ng, Andrew Y.; Jordan, Michael I (January 2003). Lafferty, John, ed. "Latent Dirichlet allocation". Journal of Machine Learning Research 3 (4–5): pp. 993–1022. doi:10.1162/jmlr.2003.3.4-5.993
#######[2]- Jo, Yohan, and Alice H. Oh. "Aspect and sentiment unification model for online review analysis." Proceedings of the fourth ACM international conference on Web search and data mining. ACM, 2011.
#######[3]- Jagarlamudi, Jagadeesh, Hal Daumé III, and Raghavendra Udupa. "Incorporating lexical priors into topic models." Proceedings of the 13th Conference of the European Chapter of the Association for Computational Linguistics. Association for Computational Linguistics, 2012.
#######[4]- Chemudugunta, C., & Steyvers, P. S. M. (2007). Modelling General and Specific Aspects of Documents with a Probabilistic Topic Model. In Advances in Neural Information Processin Systems 19: Proceedings of the 2006 Conference (Vol. 19, p. 241). MIT Press.