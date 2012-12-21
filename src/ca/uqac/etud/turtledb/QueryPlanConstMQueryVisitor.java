/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Engine;
import ca.uqac.dim.turtledb.MQueryVisitor;
import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.NAryCondition;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryPlan;
import ca.uqac.dim.turtledb.QueryVisitor;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fx
 */
public class QueryPlanConstMQueryVisitor extends MQueryVisitor
{

	private float currentCost;
	private QueryPlan currentQP = new QueryPlan();
	private String currentEngine;
	private Relation growingRel;
	Integer vTableCount = 0;

	public QueryPlan getQueryPlan()
	{
		// On ajoute le dernier morceau d'arbre au QueryPlan
		currentQP.put(currentEngine, growingRel);
		return currentQP;
	}

	@Override
	public void visit(Projection r) throws MVisitorException
	{
		r.getRelation().maccept(this);
		// On fait toujours une projection sur le même site que le sous arbre.
		growingRel = new Projection(r.getSchema(), growingRel);
	}

	@Override
	public void visit(Selection r) throws MVisitorException
	{
		r.getRelation().maccept(this);
		// On fait toujours une selection sur le même site que sur le sous arbre.
		growingRel = new Selection(r.getCondition(), growingRel);
	}

	@Override
	public void visit(Table r) throws MVisitorException
	{
		// La classe Table n'est pas utilisé dans notre implémentation
	}

	@Override
	public void visit(VariableTable r) throws MVisitorException
	{
		QueryPlan curqp;
		QueryPlan minqp = new QueryPlan();
		float curcost;
		float mincost = Float.MAX_VALUE;
		String minEngine = null;
		List<Engine> leg = BD.getTableLocations(r);

		// On recherche le site le moins cher poiur executer la recherche
		for (Engine e : leg)
		{
			curqp = new QueryPlan();
			curqp.put(e.getName(), r);
			curcost = QueryOptimizer.getCost(curqp);

			if (curcost < mincost)
			{
				minEngine = e.getName();
				mincost = curcost;
				minqp = curqp;
			}
		}

		currentCost = mincost;
		currentEngine = minEngine;
		growingRel = (Relation) r.clone();
	}

	@Override
	public void visit(Union r) throws MVisitorException
	{
		NAryvisit(r, new Union());
	}

	@Override
	public void visit(Intersection r) throws MVisitorException
	{
		NAryvisit(r, new Intersection());
	}

	@Override
	public void visit(Join r) throws MVisitorException
	{

		//On visite tout les enfants de la relation, on sauvegarde le résultat de la descente.
		r.getLeft().maccept(this);
		Relation lGrow = growingRel;
		float lCost = currentCost;
		String lEngine = currentEngine;

		r.getRight().maccept(this);
		Relation rGrow = growingRel;
		float rCost = currentCost;
		String rEngine = currentEngine;

		// si les deux fils sont sur le même site, c'est lui qu'on utilise
		if (lEngine.equals(currentEngine))
		{
			Join j = new Join(r.getCondition());
			j.setLeft(lGrow);
			j.setRight(rGrow);
			growingRel = j;
		} else
		{
			// On crée les deux scénario de plan possible
			vTableCount++;
			VariableTable v1 = new VariableTable("intermed" + vTableCount.toString(), lEngine);
			vTableCount++;
			VariableTable v2 = new VariableTable("intermed" + vTableCount.toString(), rEngine);

			v1.setRelation(rGrow);
			v2.setRelation(lGrow);

			Join j1 = new Join(r.getCondition());
			Join j2 = new Join(r.getCondition());

			j1.setLeft(lGrow);
			j1.setRight(v1);

			j2.setLeft(v2);
			j2.setRight(rGrow);

			QueryPlan QP1 = new QueryPlan(currentQP);
			QP1.put(rEngine, v1);
			QP1.put(lEngine, j1);
			float cost1 = QueryOptimizer.getCost(QP1);

			QueryPlan QP2 = new QueryPlan(currentQP);
			QP2.put(lEngine, v2);
			QP2.put(rEngine, j2);
			float cost2 = QueryOptimizer.getCost(QP2);

			if (cost1 < cost2)
			{
				growingRel = j1;
				currentQP.put(rEngine, v1);
				currentCost = cost1;
				currentEngine = lEngine;
			} else
			{
				growingRel = j2;
				currentQP.put(lEngine, v2);
				currentCost = cost2;
				currentEngine = rEngine;
			}
		}
	}

	@Override
	public void visit(Product r) throws MVisitorException
	{
		NAryvisit(r, new Product());
	}
	/* Deuxième argument : Q&D fix pour permettre d'instancier un objet dans un generics*/
	private <T extends NAryRelation> void NAryvisit(T r, T empty) throws MVisitorException
	{
		float minCost = Float.MAX_VALUE;
		QueryPlan minQP = new QueryPlan();
		String minEngine = null;
		T minTree = null;

		Map<Relation, Pair<Float, String>> costMap;
		costMap = new HashMap<Relation, Pair<Float, String>>();

		//On visite tout les enfants de la relation, on sauvegarde le résultat de la descente.
		for (Relation c : r.getRelations())
		{
			c.maccept(this);
			costMap.put(growingRel, new Pair(currentCost, currentEngine));
		}

		// Recherche du site sur lequel l'union aura un coût minimum
		for (Map.Entry<Relation, Pair<Float, String>> e : costMap.entrySet())
		{
			QueryPlan curQP = new QueryPlan(currentQP);
			String curEngine = e.getValue().getSecond();
			T candidateTree = (T) empty.clone();

			candidateTree.addOperand(e.getKey());

			for (Map.Entry<Relation, Pair<Float, String>> f : costMap.entrySet())
			{
				// On parcours toute l'entryMap sauf e
				if (e != f)
				{
					/*
					 * Si la relation f est exécuté sur le même si que e,
					 * on l'ajoute directement à l'arbre
					 */
					if (f.getValue().getSecond().equals(curEngine))
					{
						candidateTree.addOperand(f.getKey());
					} /*
					 * Sinon, on crée un sous arbre avec comme racine une VariableTable,
					 * qui pour relation f et on l'ajout et QueryPlan courant.
					 * Dans l'arbre de l'Union, on remplace la branche f par la VariableTable
					 */ else
					{
						vTableCount++;
						VariableTable v = new VariableTable(
								"intermed" + vTableCount.toString(),
								e.getValue().getSecond()); // deuxième argument : destination du VariableTable

						v.setRelation(f.getKey());
						curQP.put(f.getValue().getSecond(), v);
						candidateTree.addOperand(v);
					}
				}
			}
			QueryPlan testQP = new QueryPlan(curQP);
			testQP.put(curEngine, candidateTree);
			Float curCost = QueryOptimizer.getCost(testQP);

			if (curCost < minCost)
			{
				minCost = curCost;
				minQP = curQP;
				minEngine = curEngine;
				minTree = candidateTree;
			}
		}

		currentCost = minCost;
		currentEngine = minEngine;
		currentQP = minQP;
		growingRel = minTree;
	}
}
