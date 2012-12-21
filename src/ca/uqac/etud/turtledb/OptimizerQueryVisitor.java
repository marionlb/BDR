/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.MQueryVisitor;
import ca.uqac.dim.turtledb.MQueryVisitor.MVisitorException;
import ca.uqac.dim.turtledb.NAryCondition;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.QueryVisitor;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.UnaryRelation;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Place les Selection de manière optimale
 * @author fx
 * 
 */
public class OptimizerQueryVisitor extends QueryVisitor
{

	@Override
	public void visit(Projection prjctn) throws VisitorException
	{
		//On cherche où placer les selection
		ProjectionLocationFinderMQueryVisitor sqv = new ProjectionLocationFinderMQueryVisitor(prjctn.getSchema());
		try
		{
			prjctn.maccept(sqv);
		} catch (MVisitorException ex)
		{
			Logger.getLogger(OptimizerQueryVisitor.class.getName()).log(Level.SEVERE, null, ex);
		}
		Map<Relation, List<Relation>> pos = sqv.getProjPos();

		if (!pos.isEmpty())
		{
			prjctn.setToTrash(true);
			//On parcours la list des endroit ou placer la selection, et on la place
			for (Relation key : pos.keySet())
			{
				if (key instanceof UnaryRelation)
				{
					UnaryRelation u = ((UnaryRelation) key);
					Relation tmp = u.getRelation();
					u.setRelation(new Projection(prjctn.getSchema(), tmp));

				}
				if (key instanceof NAryRelation)
				{
					NAryRelation n = ((NAryRelation) key);
					List<Relation> concernedList = pos.get(key);
					List<Relation> nChildren = n.getRelations();
					for (int i = 0; i < nChildren.size(); i++)
					{
						if (concernedList.contains(nChildren.get(i)))
						{
							Relation tmp = nChildren.get(i);
							nChildren.set(i, new Projection(prjctn.getSchema(), tmp));
						}
					}
				}
				if (key instanceof Join)
				{
					Join j = (Join) key;
					List<Relation> concernedList = pos.get(key);

					if (concernedList.contains(j.getLeft()))
					{
						j.setLeft(new Projection(prjctn.getSchema(), j.getLeft()));

					}
					if (concernedList.contains(j.getRight()))
					{
						j.setRight(new Projection(prjctn.getSchema(), j.getRight()));
					}
				}

			}
		}
	}

	@Override
	public void visit(Selection slctn) throws VisitorException
	{
		//On cherche où placer les selection
		SelectionLocationFinderQueryVisitor sqv = new SelectionLocationFinderQueryVisitor(slctn.getCondition());
		slctn.accept(sqv);
		Map<Relation, List<Relation>> pos = sqv.getSelectPos();

		if (!pos.isEmpty())
		{
			slctn.setToTrash(true);
			//On parcours la list des endroit ou placer la selection, et on la place
			for (Relation key : pos.keySet())
			{
				if (key instanceof UnaryRelation)
				{
					UnaryRelation u = ((UnaryRelation) key);
					Relation tmp = u.getRelation();
					u.setRelation(new Selection(slctn.getCondition(), tmp));

				}
				if (key instanceof NAryRelation)
				{
					NAryRelation n = ((NAryRelation) key);
					List<Relation> concernedList = pos.get(key);
					List<Relation> nChildren = n.getRelations();
					for (int i = 0; i < nChildren.size(); i++)
					{
						if (concernedList.contains(nChildren.get(i)))
						{
							Relation tmp = nChildren.get(i);
							nChildren.set(i, new Selection(slctn.getCondition(), tmp));
						}
					}
				}
				if (key instanceof Join)
				{
					Join j = (Join) key;
					List<Relation> concernedList = pos.get(key);

					if (concernedList.contains(j.getLeft()))
					{
						j.setLeft(new Selection(slctn.getCondition(), j.getLeft()));

					}
					if (concernedList.contains(j.getRight()))
					{
						j.setRight(new Selection(slctn.getCondition(), j.getRight()));
					}
				}

			}
		}

	}

	@Override
	public void visit(Table table) throws VisitorException
	{
	}

	@Override
	public void visit(VariableTable vt) throws VisitorException
	{
	}

	@Override
	public void visit(Union union) throws VisitorException
	{
	}

	@Override
	public void visit(Intersection i) throws VisitorException
	{
	}

	@Override
	public void visit(Join join) throws VisitorException
	{
	}

	@Override
	public void visit(Product prdct) throws VisitorException
	{
	}
}
