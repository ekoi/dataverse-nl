/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
/*
 * Role.java
 *
 * Created on July 28, 2006, 2:18 PM
 *
 */
package nl.knaw.dans.dataverse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author Eko Indarto
 */
@Entity
public class RuleGoal implements java.io.Serializable  {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3345859427259666912L;
	private String dvnaliases;
	
	/**
     * Holds value of property rule.
     */
	@ManyToOne
    @JoinColumn(nullable=false)
    private Rule rule;    
    
    /** Creates a new instance of Role */
    public RuleGoal() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

	public String getDvnaliases() {
		return dvnaliases;
	}

	public void setDvnaliases(String dvnaliases) {
		this.dvnaliases = dvnaliases;
	}

	public Rule getRule() {
		return rule;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RuleGoal)) {
            return false;
        }
        RuleGoal other = (RuleGoal)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }                
}
