package ca.uqac.etud.turtledb;

import ca.uqac.dim.turtledb.BinaryRelation;
import ca.uqac.dim.turtledb.Engine;
import ca.uqac.dim.turtledb.Intersection;
import ca.uqac.dim.turtledb.Join;
import ca.uqac.dim.turtledb.MQueryVisitor;
import ca.uqac.dim.turtledb.NAryRelation;
import ca.uqac.dim.turtledb.Product;
import ca.uqac.dim.turtledb.Projection;
import ca.uqac.dim.turtledb.Relation;
import ca.uqac.dim.turtledb.Selection;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.UnaryRelation;
import ca.uqac.dim.turtledb.Union;
import ca.uqac.dim.turtledb.VariableTable;

public class CostVisitor extends MQueryVisitor {

	private float cout;
	private int nbtuples;
	private String site;

	/**
	 * Instanciation du visiteur destiné à calculer le cout d'un arbre de
	 * requêtes sur un site
	 * 
	 * @param site
	 *            Le site depuis lequel on execute la requête
	 * @param racine
	 *            L'arbre de requêtes
	 */
	public CostVisitor(String site, Relation racine) {
		this.cout = 0;
		this.nbtuples = 0;
		this.site = site;

		// initialisation des couts si besoin
		if (BD.coutsStockage == null || BD.coutsComm == null)
			for (Engine site1 : BD.sites.values()) {
				BD.addDefaultCost(site1.getName());
			}

	}

	@Override
	public void visit(VariableTable r) throws MVisitorException {
		// VariableTable : cas de base

		/*
		 * si feuille : représente un résultat déjà calculé. Il ne faut pas le
		 * prendre en compte dans le coût
		 */
		if (r.getRelation() == null && !BD.isATable(r)) {
			return;
			// TODO : vérifier que les res inter ont bien un fils nul
		}

		/*
		 * Si ce n'est pas une feuille, c'est un soit une table, soit un
		 * résultat intermédiaire à calculer et à envoyer sur un autre site
		 */
		// Cas table : on la rapatrie sur le site courant
		if (BD.isATable(r)) {
			Table t = BD.getTable(r);
			String siteT = BD.isHostedOn(t);
			this.nbtuples = t.tupleCount();

			if (siteT.equals(site)) {
				// super, on est déjà sur le bon site
			} else {
				// on calcule le cout de transfert par tuple
				float coutTransfert = cost(site, siteT);
				// on a le cout total grace au nb de tuples
				this.cout = coutTransfert * this.nbtuples;
			}
		}
		// Cas res interm. Le site dest est indiqué par l'attribut site de la
		// VariableTable
		else {
			//on visite d'abord la descendance
			r.getRelation().maccept(this);
			
			String siteD = r.getSite();
			if (siteD == null || siteD == "")
				// Anomalie, on ne fait rien
				return;

			if (siteD.equals(site)) {
				// à priori ne devrait pas arriver mais pourquoi pas
				// le cout ne change pas
			} else {
				// on calcule le cout de transfert par tuple
				float coutTransfert = cost(siteD, site);
				// ici le nb de tuples est celui calculé par les visites des
				// fils si le visiteur n'est pas tombé sur une VariableTable
				// représentant une table plutôt, on aura un cout faux
				this.cout = coutTransfert * this.nbtuples;
			}
		}

	}

	@Override
	public void visit(Projection r) throws MVisitorException {
		UnaryVisit(r);
		// pas de changement dans le nombre de tuples, ni dans le cout
	}

	@Override
	public void visit(Selection r) throws MVisitorException {
		UnaryVisit(r);
		// pas de changement dans le nombre de tuples, ni dans le cout
	}

	@Override
	public void visit(Union r) throws MVisitorException {
		NAryVisit(r);
	}

	@Override
	public void visit(Intersection r) throws MVisitorException {
		NAryVisit(r);
	}

	@Override
	public void visit(Join r) throws MVisitorException {
		BinaryVisit(r);

	}

	@Override
	public void visit(Product r) throws MVisitorException {
		NAryVisit(r);

	}

	@Override
	public void visit(Table r) throws MVisitorException {
		// ne devrait pas arriver mais pour le fun :

		if (BD.isHostedOn(r) == null)
			// la table n'est pas hébergée sur un site
			return;

		String siteT = BD.isHostedOn(r);
		this.nbtuples = r.nTuples;

		if (siteT.equals(site)) {
			// super, on est déjà sur le bon site
		} else {
			// on calcule le cout de transfert par tuple
			float coutTransfert = cost(site, siteT);
			// on a le cout total grace au nb de tuples
			this.cout = coutTransfert * this.nbtuples;
		}
	}

	public float getCout() {
		return cout;
	}

	private void UnaryVisit(UnaryRelation r) throws MVisitorException {
		r.getRelation().maccept(this);
	}

	/**
	 * Visite le fils gauche et le fils droit de la BinaryRelation
	 * 
	 * @param r
	 * @throws MVisitorException
	 */
	private void BinaryVisit(BinaryRelation r) throws MVisitorException {
		r.getLeft().maccept(this);
		r.getRight().maccept(this);
	}

	/**
	 * Viste les n fils de la NAryRelation
	 * 
	 * @param r
	 * @throws MVisitorException
	 */
	private void NAryVisit(NAryRelation r) throws MVisitorException {
		for (Relation relation : r.getRelations()) {
			relation.maccept(this);
		}
	}

	public static float cost(String siteDest, String siteInitial) {
		float res = 0;
		if (siteDest.equals(siteInitial))
			;
		else if (!BD.coutsStockage.containsKey(siteDest))
			;
		else if (BD.coutsComm.get(siteDest, siteInitial) < 0)
			;
		else
			res = BD.coutsStockage.get(siteDest)
					+ BD.coutsComm.get(siteDest, siteInitial);
		assert res >= 0;
		return res;
	}

}
