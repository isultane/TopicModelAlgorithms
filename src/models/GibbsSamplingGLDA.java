package models;
/**
 * UNDER PROCESS & MAINTENANCE
 * @author Sultan Alqahtani
 */
public class GibbsSamplingGLDA {

	/**
	 * Gipps sampling equations, given by the model's author
	 * p(z_i = j | d) = (Number of tokens with topic j in doc d + dAlpha) / (Number of tokens in doc d + dAlpha*K)
	 * p(x_i = 1| z) = tau and p(x_i=0|z) = 1- tau  (I tried to learn these values but, if I remember correctly, that didn't help)
	 * p(w_i | z_i=j, x_i=1) = (Number of word tokens of word wi with seed topic j + dMu) / (Number of seed tokens in topic j + dMu*W) 
	 * p(w_i | z_i=j, x_i=0) = (Number of word tokens of word wi with topic j + dBeta) / (Number of tokens in topic j + dBeta * W)
	 */
}
