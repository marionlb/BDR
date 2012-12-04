import java.util.List;

import ca.uqac.dim.turtledb.QueryPlan;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.VariableTable;

public class QueryOptimizer {

	/**
	 * Le nom des sites composant la base de données répartie
	 */
	List<String> listeSites;

	/**
	 * Le nom et la composition (i.e. expression d'algèbre relationnelle) de
	 * chacun des fragments + La solution d'allocation des fragments sur chacun
	 * des sites (i.e. quel fragment est hébergé sur quel site)
	 */
	List<VariableTable> listeFragments;

	/**
	 * Tableau des coûts de stockage
	 */
	float[] coutsStockage;
	/**
	 * Matrice des coûts de communication
	 */
	float[][] coutsComm;

	/**
	 * Calcule le coût d'un plan de requêtes donné, à partir des coûts de
	 * stockage et de communication prédéfinis.
	 * 
	 * @param qp
	 *            Le plan de requête qu'on analyse
	 * @return Le coût total du plan de requêtes.
	 */
	public int getCost(QueryPlan qp) {
		return 0;
	}

	/**
	 * Calcule le meilleur plan de requêtes distribué possible pour une requête,
	 * en matière de coût
	 * 
	 * @param r
	 *            L'arbre d'algèbre relationnelle de la requête
	 * @return Le plan de requêtes optimisé
	 */
	public QueryPlan optimizeQuery(Relation r) {
		return null;
	}

	public QueryOptimizer(List<String> listeSites,
			List<VariableTable> listeFragments, float[] coutsStockage,
			float[][] coutsComm) {
		super();
		this.listeSites = listeSites;
		this.listeFragments = listeFragments;
		this.coutsStockage = coutsStockage;
		this.coutsComm = coutsComm;
	}

	// //////////////////////////////////////////////
	// ////////GETTERS ++ SETTERS////////////////////
	// //////////////////////////////////////////////
	public List<String> getListeSites() {
		return listeSites;
	}

	public void setListeSites(List<String> listeSites) {
		this.listeSites = listeSites;
	}

	public List<VariableTable> getListeFragments() {
		return listeFragments;
	}

	public void setListeFragments(List<VariableTable> listeFragments) {
		this.listeFragments = listeFragments;
	}

	public float[] getCoutsStockage() {
		return coutsStockage;
	}

	public void setCoutsStockage(float[] coutsStockage) {
		this.coutsStockage = coutsStockage;
	}

	public float[][] getCoutsComm() {
		return coutsComm;
	}

	public void setCoutsComm(float[][] coutsComm) {
		this.coutsComm = coutsComm;
	}
}
