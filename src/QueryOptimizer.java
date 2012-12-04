import java.util.ArrayList;
import java.util.List;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryPlan;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Schema;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.TableParser;
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
	
	static QueryOptimizer randomInit(int nbSites, int nbTables) {
		//init
		List<String> lnomSites = new ArrayList<String>();
		List<VariableTable> lfrag = new ArrayList<VariableTable>();
		float[] cStock = new float[nbSites];
		float[][] cComm = new float[nbSites][nbSites];
		
		//init des sites
		for (int i = 0; i < nbSites; i++) {
			lnomSites.add("Site"+i);
		}
		
		//init des fragments
		int nbfrag, site, nbAtt;
		char car;
		VariableTable fragment;
		Table table;
		for (int i = 0; i < nbTables; i++) {
			car=(char) ('A'+i);
			
			//Création de la table
			Schema schema = new Schema();
			//entre 1 et 5 attributs
			nbAtt = 1 +(int) (Math.random()*5);
			for (int j = 0; j < nbAtt; j++) {
				schema.add(new Attribute(""+car, "att"+j));
			}
			//on crée la table vide
			table = TableParser.parseFromCsv(""+car, "");
			/* dû modifié la visibilité de setSchema */
			table.setSchema(schema);
			
			//Création des fragments
			//entre 1 et 4 fragments
			nbfrag = 1 + (int) (Math.random()*4);
			
			for (int j = 0; j < nbfrag; j++) {
				site = (int) (Math.random()*nbSites);
				fragment = new VariableTable(""+car+j,lnomSites.get(site));
				//la projection n'est pas exacte, mais c'est le principe qui compte :)
				fragment.setRelation(new Projection(schema, table));
				lfrag.add(fragment);
			}
		}
		
		
		return new QueryOptimizer(lnomSites, lfrag, cStock, cComm);
	}
	
	public static void main(String[] args)  {
		randomInit(4, 5);
	}
}
