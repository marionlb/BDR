package ca.uqac.etud.turtledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import ca.uqac.dim.turtledb.Attribute;
import ca.uqac.dim.turtledb.BinaryRelation;
import ca.uqac.dim.turtledb.Engine;
import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.MQueryVisitor.MVisitorException;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryPlan;
import ca.uqac.dim.turtledb.QueryVisitor.VisitorException;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Schema;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.TableParser;
import ca.uqac.dim.turtledb.UnaryRelation;
import ca.uqac.dim.turtledb.VariableTable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueryOptimizer {

	/**
	 * Tableau des coûts de stockage
	 */
	private static HashMap<String, Float> coutsStockage;
	/**
	 * Matrice des coûts de communication
	 */
	private static Matrice coutsComm;

	//éventuellement virer la mémorisation des couts des résultats intermédiaires
	private static HashMap<String, Float> varInterCouts;
	
	/**
	 * Calcule le coût d'un plan de requêtes donné, à partir des coûts de
	 * stockage et de communication prédéfinis.
	 * 
	 * @param qp
	 *            Le plan de requête qu'on analyse
	 * @return Le coût total du plan de requêtes.
	 */
	public static float getCost(QueryPlan qp) {
		if (coutsStockage == null || coutsComm == null)
			for (Engine site : BD.sites.values()) {
				QueryOptimizer.addDefaultCost(site.getName());
			}

		float res = 0;
		varInterCouts = new HashMap<String, Float>();
		for (Entry<String, Set<Relation>> entry : qp.entrySet()) {
			res += cost(entry.getKey(), entry.getValue());
		}
		return res;
	}

	public static float cost(String site, Set<Relation> set) {
		float res = 0;
		if (!BD.sites.containsKey(site))
			return -1;

		for (Iterator<Relation> iterator = set.iterator(); iterator.hasNext();) {
			Relation r = iterator.next();

			if (r instanceof VariableTable && !BD.isATable((VariableTable) r)) {
				VariableTable vt = (VariableTable) r;

				res += calcCost(site, vt.getRelation());

				if (vt.getSite() != null && vt.getSite() != "") {
					String siteTransfert = vt.getSite();
					if (BD.sites.containsKey(siteTransfert)) {
						float coutTransfert = cost(siteTransfert, site);
						System.out.println(vt.getName() + " : "
								+ vt.getRelation().nTuples + " tuples");
						coutTransfert *= vt.getRelation().nTuples;
						res += coutTransfert;
					}
				}
				varInterCouts.put(((VariableTable) r).getName(), res);
			} else
				res = calcCost(site, r);
		}
		System.out.println(site + " : " + res);
		return res;
	}

	private static float calcCost(String site, Relation r) {
		float res = 0;

		// cas R�sultat interm�diaire feuille : cout d�j� calcul�
		if (r instanceof VariableTable && !BD.isATable((VariableTable) r)) {
			// rien à faire, on a déjà integré le cout

			// VariableTable vt = (VariableTable) r;
			// if (!varInterCouts.containsKey(vt.getName())) {
			// return -3;
			// }
			// float cost = varInterCouts.get(vt.getName());
			// vt.nTuples = cost;
			// res=cost;
		}
		// cas Table : transfert des tuples
		else if (/* r instanceof Table || */r instanceof VariableTable
				&& BD.isATable((VariableTable) r)) {
			// cout = nb tuples d�plac�s * cout de deplacement
			// Table table = (r instanceof Table ? (Table) r :
			// BD.tables.get(((VariableTable)r).getName()));
			Table table = BD.getTable((VariableTable) r);
			if (BD.isHostedOn(table) == null)
				return -2;
			String[] siteNames = BD.isHostedOn(table).split("\n");
			float coutMin = Float.MAX_VALUE;
			for (String siteName : siteNames) {
				if (siteName.equals(site)) {
					coutMin = 0;
					break;
				} else if (cost(site, siteName) < coutMin) {
					coutMin = cost(site, siteName);
				}
			}
			r.nTuples = table.tupleCount();
			System.out
					.println(table.getName() + " : " + r.nTuples + " tuples.");
			res = coutMin * r.nTuples;

		} else if (r instanceof Join) {
			Join j = (Join) r;
			res = calcCost(site, j.getLeft());
			res += calcCost(site, j.getRight());

			//cas Join
			if(j.getCondition()!=null) {
				r.nTuples = Math.max(j.getLeft().nTuples,j.getRight().nTuples);
			}
			//cas Produit Cartésien
			else {
				r.nTuples = j.getLeft().nTuples * j.getRight().nTuples;
			}

		} else if (r instanceof NAryRelation) {
			
			for (Relation rel : ((NAryRelation) r).getRelations()) {
				res += calcCost(site, rel);
				if(r instanceof Intersection) {
					r.nTuples = Math.min(r.nTuples, rel.nTuples);
				} else if(r instanceof Product) {
					r.nTuples = r.nTuples * rel.nTuples;
				} else /*if Union*/ {
					r.nTuples = Math.max(r.nTuples, rel.nTuples);
				}		
			}
		} else if (r instanceof UnaryRelation) {
			res = calcCost(site, ((UnaryRelation) r).getRelation());
			//même chose dans cas Sélection et Projection
			r.nTuples = ((UnaryRelation) r).getRelation().nTuples;
		}
		r.cost = res;
		return res;
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
	public static Relation getOptimizeRelation(Relation r) throws VisitorException {
		OptimizerQueryVisitor oqv = new OptimizerQueryVisitor();
		r.accept(oqv);
		CleanerQueryVisitor cqv = new CleanerQueryVisitor();
		r.accept(cqv);
		return r;
		
	}

	public static float cost(String siteDest, String s2) {
		return coutsStockage.get(siteDest) + coutsComm.get(siteDest, s2);
	}

	public static void addDefaultCost(String site1) {
		if (coutsStockage == null)
			coutsStockage = new HashMap<String, Float>();
		if (coutsComm == null)
			coutsComm = new Matrice();

		// cout stockage
		coutsStockage.put(site1, (float) 0.01);
		// cout comm
		for (Engine site : BD.sites.values()) {
			if (!site.getName().equals(site1))
				coutsComm.set(site1, site.getName(), 1);
		}
	}
}

class Matrice extends HashMap<String, Float> {

	public Matrice() {
		super();
		// for (Engine site : BD.sites.values()) {
		// QueryOptimizer.addDefaultCost(site.getName());
		// }
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