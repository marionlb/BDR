package ca.uqac.etud.turtledb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.uqac.dim.turtledb.QueryPlan;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.UnaryRelation;
import ca.uqac.dim.turtledb.MQueryVisitor.MVisitorException;
import ca.uqac.dim.turtledb.QueryVisitor.VisitorException;

public class QueryOptimizer {

	/**
	 * Calcule le coût en pire cas d'un {@link QueryPlan}. 
	 * Utilise un {@link CostVisitor} pour ne pas modifier l'arbre de requêtes.
	 * @param qp Le QueryPlan dont le coût est à calculer. 
	 * @return Le coût du plan de requêtes
	 */
	public static float getCost(QueryPlan qp) {
		float totalCost = 0;
		for (Iterator<Entry<String,Set<Relation>>> iterator = qp.entrySet().iterator(); iterator.hasNext();) {
			
			Entry<String,Set<Relation>> next = iterator.next();
			String siteExec = (String) next.getKey();
			Set<Relation> setRel = next.getValue();
			
			CostVisitor cv;
			
			for (Relation relation : setRel) {
				cv = new CostVisitor(siteExec, relation);
				try {
					relation.maccept(cv);
				} catch (MVisitorException e) {
					e.printStackTrace();
				}
				totalCost+=cv.getCout();
			}
		}
		return totalCost;
	}

	/**
	 * Calcule le meilleur plan de requêtes distribué possible pour une requête,
	 * en matière de coût
	 * 
	 * @param r
	 *            L'arbre d'algèbre relationnelle de la requête
	 * @return Le plan de requêtes optimisé
	 */
	public static QueryPlan optimizeQuery(Relation r) {
		try
		{
			r = getOptimizeRelation(r);
			QueryPlanConstMQueryVisitor qpc = new QueryPlanConstMQueryVisitor();
			r.maccept(qpc);

			return qpc.getQueryPlan();

		} catch (MVisitorException ex)
		{
			Logger.getLogger(QueryOptimizer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (VisitorException ex)
		{
			Logger.getLogger(QueryOptimizer.class.getName()).log(Level.SEVERE, null, ex);
		}


		return null;

	}
	
	/**
	 * Optimize un arbre de requêtes sans tenir compte des sites ou de l'état de la BD.
	 * *Descends les selections    
	 * *Organise les projections    
	 * @param r L'arbre de requêtes à optimiser
	 * @return L'arbre optimiser
	 * @throws VisitorException
	 */
	public static Relation getOptimizeRelation(Relation r) throws VisitorException {
		OptimizerQueryVisitor oqv = new OptimizerQueryVisitor();
		r.accept(oqv);
		CleanerQueryVisitor cqv = new CleanerQueryVisitor();
		r.accept(cqv);
		
		// Trash la racine si besoin
		if (r instanceof UnaryRelation)
		{
			UnaryRelation u = (UnaryRelation) r;
			if (u.isToTrash())
				r = u.getRelation();
			
		}
		return r;

	}
}

/**
 * Structure de données représentant une matrice 2x2. 
 * Utilisée pour les couts de communication, qui dépendent à la fois
 * du site destination et du site initial 
 * @author marion
 *
 */
class Matrice extends HashMap<String, Float> {

	public Matrice() {
		super();
	}

	private void add(String site1, String site2, float cout) {
		this.put(site1 + "-" + site2, cout);
	}

	public float get(String site1, String site2) {
		if (this.containsKey(site1 + "-" + site2)) {
			return this.get(site1 + "-" + site2);
		} else if (this.containsKey(site2 + "-" + site1)) {
			return this.get(site2 + "-" + site1);
		}
		return -1;
	}

	public void set(String site1, String site2, float cout) {
		if (!this.containsKey(site1 + "-" + site2)
				&& !this.containsKey(site2 + "-" + site1)) {
			this.add(site1, site2, cout);
		} else {
			if (this.containsKey(site1)) {
				this.remove(site1);
			} else {
				this.remove(site2);
			}
			this.add(site1, site2, cout);
		}
	}

}