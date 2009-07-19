/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package schooltimetablecsp;

import java.util.List;
import java.util.Iterator;
import java.util.Collection;

/**
 *
 * @author five_stars
 */
public class Heuristic
{

    SpreadConstraint spread_day;
    DayConstraint day;
    LastHourConstraint not_last;
    DoubleHourConstraint double_hour;
    ConsecutiveHourConstraint cont_hour;

    public Heuristic(double sc, double lhc)
    {
        this.spread_day = new SpreadConstraint(sc);
        this.day        = new DayConstraint();
        this.not_last   = new LastHourConstraint(lhc);
        this.double_hour= new DoubleHourConstraint();
        this.cont_hour  = new ConsecutiveHourConstraint();
        
    }
    
    public Dispo assignSlot(Subject ss, List<Dispo> tried)
    {
        //scorri la lista delle slot e verifica se e' compatibile con quelle gia' assegnata a ss
        Iterator it = ss.dominio.iterator();
        while (it.hasNext())
        {
            Dispo dd = (Dispo)it.next();
            if (dd.s == null && this.goodSlot(ss,dd) && !tried.contains(dd))
            {
                dd.s = ss;
                ss.assigned.add(dd);
                ss.n_slot_assigned ++;
                ss.dominio.remove(dd);
                return dd;
            }
            else
            {
            }
        }
        return null;
    }

    public void generateChild(Node daddy, List<Subject> ss)
    {
        Iterator it = ss.iterator();
        while (it.hasNext())
        {
            Subject temp_s = (Subject) it.next();
            if (temp_s.n_slot_assigned < temp_s.n_slot)
            {
                Node child = new Node(daddy, temp_s);
                this.addChild(daddy, child);
            }
        }
    }

    public void addChild(Node daddy, Node child)
    {
        boolean added = false;
        if (daddy.hasChildren())
        {
            Collection<Node> daddy_child = daddy.figli();
            Subject child_ss = (Subject)child.value();
            Iterator it = daddy_child.iterator();
            while (it.hasNext())
            {
                Node temp_n = (Node) it.next();
                Subject temp_s = (Subject)temp_n.value();
                //// numero possibili assegnazioni / numero ore da assegnare
                double temp_val = (double)temp_s.dominio.size()/(double)(temp_s.n_slot - temp_s.n_slot_assigned);
                //// numero possibili assegnazioni / numero ore da assegnare
                double res_val = (double)child_ss.dominio.size()/(double)(child_ss.n_slot - child_ss.n_slot_assigned);
                //System.out.print(" (" +child_ss.dominio.size()+")/("+child_ss.n_slot+"-"+child_ss.n_slot_assigned+")="+res_val);
                //System.out.print(" (" +temp_s.dominio.size()+")/("+temp_s.n_slot+"-"+temp_s.n_slot_assigned+")="+temp_val);
                //System.out.println();
                if (temp_val <= res_val)
                {
                    //System.out.println(" - >  assign " + (child_ss) + " after " + temp_s);
                    daddy.insertChildren(child, daddy.getChildIndex(temp_n) ); //addChildren(child);
                    added = true;
                    break;
                }
            }
        }
        if (!added)
        {
            //System.out.println(" - >  assign " + ((Subject)child.value()) + " at the end");
            daddy.addChildren(child);
        }
    }

    public boolean goodSlot(Subject ss, Dispo dd)
    {
        boolean res = true;

        Iterator it = ss.assigned.iterator();
        while (it.hasNext())
        {
            Dispo temp_d = (Dispo) it.next();
            // verifica se gli oggetti sono diversi
            if (temp_d != dd && dd != null && temp_d.key != dd.key)
            {
                //verifica se sono nello stesso giorno
                if (temp_d.j.getNum() == dd.j.getNum())
                {
                    //verifica se il corso ammette piu' di due ore consecutive
                    if ( this.double_hour.valid(ss, dd) )
                    {
                        //verifica se le ore sono consecutive
                        if ( this.cont_hour.valid(temp_d, dd) )
                        {
                            //verifica che non ci siano gia' piu' di due ore in quel giorno
                            if ( this.day.valid(ss, temp_d.j) < 2 )
                            {
                                //se non ci sono => OK
                                res = true ;
                            }
                            else
                            {
                                res = false;
                                break;
                            }
                        }
                        else
                        {
                            res = false;
                            break;
                        }
                    }
                    else
                    {
                        res = false;
                        break;
                    }
                }
                else
                {
                    //verifica la distribuzione delle ore (eg: non sempre la stessa)
                    if ( this.not_last.valid(ss, dd) )
                    {
                        //TODO: deve diventare un soft constraint => peso
                        //verifica la distribuzione nei giorni (non giorni consecutivi)
                        if ( this.spread_day.valid(ss, dd.j)==0 )
                        {
                            //verifica che non ci siano gia' piu' di due ore in quel giorno
                            if (this.day.valid(ss, dd.j) < 2)
                            {
                                res = res && true;
                            }
                            else
                            {
                                res = false;
                                break;
                            }
                        }
                        else
                        {
                            res = false;
                            break;
                        }
                    }
                    else
                    {
                        res = false;
                        break;
                    }
                }
            }
            else
            {
                res = false;
                break;
            }
        }
        return res;
    }

    public boolean checkConflict(Dispo d1, Dispo d2)
    {
        // verifica se gli oggetti sono diversi
        if (d1 != d2 && d1 != null && d1.key != d2.key)
        {
            /**
            * STESSO GIORNO
            */
            //verifica se sono nello stesso giorno
            if (d1.j.getNum() == d2.j.getNum())
            {//verifica se il corso ammette piu' di due ore consecutive
                if ( this.double_hour.valid(d1.s, d1) )
                {
                    if ( this.cont_hour.valid(d1, d2) )
                    {
                        return true;
                    }
                }
            }
            /**
             * GIORNI ADIACENTI AD ORE DIVERSE
             */
            else
            {
                //verifica la distribuzione delle ore (eg: non sempre la stessa)
                if ( this.not_last.valid(d1.s, d2) )
                {
                    if ( this.spread_day.valid(d1.s, d2.j)==0 )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
