package crazydev.meteo;

import java.util.HashMap;
import java.util.Map;

public abstract class MeteoDepartments
{
    private static final Map<Integer, String> DEPARTMENTS = new HashMap<>();

    static
    {
        DEPARTMENTS.put( 1, "Ain");
        DEPARTMENTS.put( 2, "Aisne");
        DEPARTMENTS.put( 3, "Allier");
        DEPARTMENTS.put( 4, "Alpes-de-Haute-Provence");
        DEPARTMENTS.put( 5, "Hautes-Alpes");
        DEPARTMENTS.put( 6, "Alpes-Maritimes");
        DEPARTMENTS.put( 7, "Ardèche");
        DEPARTMENTS.put( 8, "Ardennes");
        DEPARTMENTS.put( 9, "Ariège");
        DEPARTMENTS.put( 10, "Aube");
        DEPARTMENTS.put( 11, "Aude");
        DEPARTMENTS.put( 12, "Aveyron");
        DEPARTMENTS.put( 13, "Bouches-du-Rhône");
        DEPARTMENTS.put( 14, "Calvados");
        DEPARTMENTS.put( 15, "Cantal");
        DEPARTMENTS.put( 16, "Charente");
        DEPARTMENTS.put( 17, "Charente-Maritime");
        DEPARTMENTS.put( 18, "Cher");
        DEPARTMENTS.put( 19, "Corrèze");

        DEPARTMENTS.put( 20, "Corse");
        // 2A : Corse-du-Sud
        // 2B : Haute-Corse

        DEPARTMENTS.put( 21, "Côte-d'Or");
        DEPARTMENTS.put( 22, "Côtes-d'Armor");
        DEPARTMENTS.put( 23, "Creuse");
        DEPARTMENTS.put( 24, "Dordogne");
        DEPARTMENTS.put( 25, "Doubs");
        DEPARTMENTS.put( 26, "Drôme");
        DEPARTMENTS.put( 27, "Eure");
        DEPARTMENTS.put( 28, "Eure-et-Loir");
        DEPARTMENTS.put( 29, "Finistère");
        DEPARTMENTS.put( 30, "Gard");
        DEPARTMENTS.put( 31, "Haute-Garonne");
        DEPARTMENTS.put( 32, "Gers");
        DEPARTMENTS.put( 33, "Gironde");
        DEPARTMENTS.put( 34, "Hérault");
        DEPARTMENTS.put( 35, "Ille-et-Vilaine");
        DEPARTMENTS.put( 36, "Indre");
        DEPARTMENTS.put( 37, "Indre-et-Loire");
        DEPARTMENTS.put( 38, "Isère");
        DEPARTMENTS.put( 39, "Jura");
        DEPARTMENTS.put( 40, "Landes");
        DEPARTMENTS.put( 41, "Loir-et-Cher");
        DEPARTMENTS.put( 42, "Loire");
        DEPARTMENTS.put( 43, "Haute-Loire");
        DEPARTMENTS.put( 44, "Loire-Atlantique");
        DEPARTMENTS.put( 45, "Loiret");
        DEPARTMENTS.put( 46, "Lot");
        DEPARTMENTS.put( 47, "Lot-et-Garonne");
        DEPARTMENTS.put( 48, "Lozère");
        DEPARTMENTS.put( 49, "Maine-et-Loire");
        DEPARTMENTS.put( 50, "Manche");
        DEPARTMENTS.put( 51, "Marne");
        DEPARTMENTS.put( 52, "Haute-Marne");
        DEPARTMENTS.put( 53, "Mayenne");
        DEPARTMENTS.put( 54, "Meurthe-et-Moselle");
        DEPARTMENTS.put( 55, "Meuse");
        DEPARTMENTS.put( 56, "Morbihan");
        DEPARTMENTS.put( 57, "Moselle");
        DEPARTMENTS.put( 58, "Nièvre");
        DEPARTMENTS.put( 59, "Nord");
        DEPARTMENTS.put( 60, "Oise");
        DEPARTMENTS.put( 61, "Orne");
        DEPARTMENTS.put( 62, "Pas-de-Calais");
        DEPARTMENTS.put( 63, "Puy-de-Dôme");
        DEPARTMENTS.put( 64, "Pyrénées-Atlantiques");
        DEPARTMENTS.put( 65, "Hautes-Pyrénées");
        DEPARTMENTS.put( 66, "Pyrénées-Orientales");
        DEPARTMENTS.put( 67, "Bas-Rhin");
        DEPARTMENTS.put( 68, "Haut-Rhin");
        DEPARTMENTS.put( 69, "Rhône");
        DEPARTMENTS.put( 70, "Haute-Saône");
        DEPARTMENTS.put( 71, "Saône-et-Loire");
        DEPARTMENTS.put( 72, "Sarthe");
        DEPARTMENTS.put( 73, "Savoie");
        DEPARTMENTS.put( 74, "Haute-Savoie");
        DEPARTMENTS.put( 75, "Paris");
        DEPARTMENTS.put( 76, "Seine-Maritime");
        DEPARTMENTS.put( 77, "Seine-et-Marne");
        DEPARTMENTS.put( 78, "Yvelines");
        DEPARTMENTS.put( 79, "Deux-Sèvres");
        DEPARTMENTS.put( 80, "Somme");
        DEPARTMENTS.put( 81, "Tarn");
        DEPARTMENTS.put( 82, "Tarn-et-Garonne");
        DEPARTMENTS.put( 83, "Var");
        DEPARTMENTS.put( 84, "Vaucluse");
        DEPARTMENTS.put( 85, "Vendée");
        DEPARTMENTS.put( 86, "Vienne");
        DEPARTMENTS.put( 87, "Haute-Vienne");
        DEPARTMENTS.put( 88, "Vosges");
        DEPARTMENTS.put( 89, "Yonne");
        DEPARTMENTS.put( 90, "Territoire de Belfort");
        DEPARTMENTS.put( 91, "Essonne");
        DEPARTMENTS.put( 92, "Hauts-de-Seine");
        DEPARTMENTS.put( 93, "Seine-Saint-Denis");
        DEPARTMENTS.put( 94, "Val-de-Marne");
        DEPARTMENTS.put( 95, "Val-d'Oise");
    }

    private MeteoDepartments()
    {
    }

    public static String getName(int dept)
    {
        final String name = DEPARTMENTS.get(dept);

        if(name == null)
        {
            throw new RuntimeException("OUCH!");
        }

        return name;
    }
}
