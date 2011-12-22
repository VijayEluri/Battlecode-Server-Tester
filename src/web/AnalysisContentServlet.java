package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.BSMatch;

import common.HibernateUtil;

public class AnalysisContentServlet extends HttpServlet {
	private static final long serialVersionUID = -3373145024382759806L;
	public static final String NAME = "analysis_content.html";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		PrintWriter out = response.getWriter();
		out.println("<html><head>");
		out.println("<title>Battlecode Tester</title>");
		out.println("<script src='/js/jquery-1.7.1.min.js'></script>");
		out.println("<script src='/js/jquery-ui-1.8.16.custom.min.js'></script>");
		out.println("<link rel='stylesheet' href='/css/jquery-ui-1.8.16.custom.css' />");
		out.println("<link rel='stylesheet' href='/css/jquery-ui.css' />");
		out.println("</head>");
		out.println("<body>");
		String strId = request.getParameter("id");
		if (strId == null || !strId.matches("\\d+")) {
			out.println("Invalid id</body></html>");
			return;
		}
		long id = new Long(Integer.parseInt(strId));
		EntityManager em = HibernateUtil.getEntityManager();
		BSMatch match = em.find(BSMatch.class, id);
		printContent(request, response, match);
	}

	public static void printContent(HttpServletRequest request, HttpServletResponse response, BSMatch match) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		out.println("<link rel=\"stylesheet\" href=\"css/jquery.jqplot.min.css\" />");
		out.println("<script src='js/jquery.jqplot.min.js'></script>");
		out.println("<script src='js/jqplot.cursor.min.js'></script>");

		out.println("<h2 style='text-align:center'><font color='red'>" + match.getRun().getTeamA().getPlayerName() + "</font> vs. <font color='blue'>" + 
				match.getRun().getTeamB().getPlayerName() + "</font></h2>");
		out.println("<h2 style='text-align:center'>" + match.getMap().getMapName() + "</h2>");
		
		out.println("<script type='text/javascript'>" +
				"var dataMap = [];" +
				"</script>");
		printArray(out, "aFluxIncome", match.getResult().getaResult().getFluxIncome());
		printArray(out, "aFluxDrain", match.getResult().getaResult().getFluxDrain());
		printArray(out, "aFluxReserve", match.getResult().getaResult().getFluxReserve());
		printArray(out, "aActiveRobots", match.getResult().getaResult().getActiveRobots());
		
		printArray(out, "bFluxIncome", match.getResult().getbResult().getFluxIncome());
		printArray(out, "bFluxDrain", match.getResult().getbResult().getFluxDrain());
		printArray(out, "bFluxReserve", match.getResult().getbResult().getFluxReserve());
		printArray(out, "bActiveRobots", match.getResult().getbResult().getActiveRobots());
		out.print("<script type='text/javascript'>" +
				"var rounds = " + match.getResult().getRounds() + 
				"</script>");
		
		out.println("<div id='buttonWrapper' style='height:70px; text-align:center'>");
		out.println("<div id='aViewButtons' style='margin-left:15px; float:left'>" +
				"<input type='radio' id='aFluxIncome' name='aFluxIncome' checked='checked' /><label for='aFluxIncome'>Flux Income</label>" +
				"<input type='radio' id='aFluxDrain' name='aFluxDrain' /><label for='aFluxDrain'>Flux Drain</label>" +
				"<input type='radio' id='aFluxReserve' name='aFluxReserve' /><label for='aFluxReserve'>Flux Reserve</label>" +
				"<input type='radio' id='aActiveRobots' name='aActiveRobots' /><label for='aActiveRobots'>Active Robots</label>" +
				" </div>");
		out.println("<div id='bViewButtons' style='margin-left:20px; float:right'>" +
				"<input type='radio' id='bFluxIncome' name='bFluxIncome' checked='checked' /><label for='bFluxIncome'>Flux Income</label>" +
				"<input type='radio' id='bFluxDrain' name='bFluxDrain' /><label for='bFluxDrain'>Flux Drain</label>" +
				"<input type='radio' id='bFluxReserve' name='bFluxReserve' /><label for='bFluxReserve'>Flux Reserve</label>" +
				"<input type='radio' id='bActiveRobots' name='bActiveRobots' /><label for='bActiveRobots'>Active Robots</label>" +
				" </div>");
		out.println("<button id='resetZoom' style='margin-top:10px'>Reset zoom</button>");
		out.println("</div>");
		out.println("<div id='chart' style='height: 400px; width:100%'></div>");
		
		
		out.println("<script src='js/analysis.js'></script>");
		out.println("</body></html>");
	}
	
	@SuppressWarnings("unchecked")
	private static void printArray(PrintWriter out, String name, List list) {
		out.print("<script type='text/javascript'>" +
				"dataMap['" + name + "'] = [");
		for (int i = 0; i < list.size(); i++) {
			out.print("[" + i + "," + list.get(i) + "],");
		}
		out.println("];" +
				"</script>");
	}
	
	
	@Override
	public String toString() {
		return NAME;
	}
}
