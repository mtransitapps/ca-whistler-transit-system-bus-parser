package org.mtransit.parser.ca_whistler_transit_system_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.commons.StrategicMappingCommons;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://bctransit.com/*/footer/open-data
// https://bctransit.com/servlet/bctransit/data/GTFS - Whistler
// https://whistler.mapstrat.com/current/google_transit.zip
public class WhistlerTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-whistler-transit-system-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new WhistlerTransitSystemBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Whistler Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating Whistler Transit System bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}


	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "1"; // Whistler Transit System only

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!INCLUDE_AGENCY_ID.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final long RID_ENDS_WITH_W = 23_000_000L;
	private static final long RID_ENDS_WITH_X = 24_000_000L;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
				String rsn = gRoute.getRouteShortName().toLowerCase(Locale.ENGLISH);
				if (rsn.endsWith("w")) {
					return digits + RID_ENDS_WITH_W;
				} else if (rsn.endsWith("x")) {
					return digits + RID_ENDS_WITH_X;
				}
				System.out.printf("\nUnexptected route ID for %s!\n", gRoute);
				System.exit(-1);
				return -1l;
			}
		}
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		if (StringUtils.isEmpty(routeLongName)) {
			routeLongName = gRoute.getRouteDesc();
		}
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				if ("20X".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return "0C4D8C";
				} else if ("25X".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return "EB1D8D";
				}
			}
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 4: return "00AA4F";
			case 5: return "8E0D3A";
			case 6: return "FDC215";
			case 7: return "B3AA7E";
			case 8: return "F49AC1";
			case 20: return "0C4D8C";
			case 21: return "F68A20";
			case 25: return "EB1D8D";
			case 30: return "27ABE2";
			case 31: return "A44499";
			case 32: return "8CC640";
			case 99: return "5C87A1";
			// @formatter:on
			default:
				if (super.isGoodEnoughAccepted()) {
					return AGENCY_COLOR_BLUE;
				}
				System.out.printf("\nUnexpected route color %s!\n", gRoute);
				System.exit(-1);
				return null;
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(31L, new RouteTripSpec(31L, //
				StrategicMappingCommons.NORTH, MTrip.HEADSIGN_TYPE_STRING, "Alpine", //
				StrategicMappingCommons.SOUTH, MTrip.HEADSIGN_TYPE_STRING, "Village") //
				.addTripSort(StrategicMappingCommons.NORTH, //
						Arrays.asList(new String[] { //
						"118", // "102714", // Gondola Exchange Bay 3
								"102", // "102622", // Alpine at Rainbow (WB)
						})) //
				.addTripSort(StrategicMappingCommons.SOUTH, //
						Arrays.asList(new String[] { //
						"102", // "102622", // Alpine at Rainbow (WB)
								"118", // "102714", // Gondola Exchange Bay 3
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 4L) {
			if (gTrip.getDirectionId() == 1) { // Marketplace - Free Shuttle - ????
				if ("Marketplace - Free Shuttle".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.COUNTERCLOCKWISE);
					return;
				}
			}
		} else if (mRoute.getId() == 5L) {
			if (gTrip.getDirectionId() == 0) { // Upper Vlg - Benchlands - CLOCKWISE
				if ("Upper Vlg - Benchlands - Free Shuttle".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.CLOCKWISE);
					return;
				}
			}
		} else if (mRoute.getId() == 6L) {
			if (gTrip.getDirectionId() == 1) { // Tapley''s-Blueberry - COUNTERCLOCKWISE
				if ("Tapley's-Blueberry".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.COUNTERCLOCKWISE);
					return;
				}
			}
		} else if (mRoute.getId() == 7L) {
			if (gTrip.getDirectionId() == 0) { // Staff Housing - CLOCKWISE
				if ("Staff Housing".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.CLOCKWISE);
					return;
				}
			}
		} else if (mRoute.getId() == 20L) {
			if (gTrip.getDirectionId() == 0) { // Village - NORTH
				if ("To Village".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "To Village-Via Function Jct".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Cheakamus - SOUTH
				if ("Cheakamus".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Cheakamus- Via Function Jct".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (mRoute.getId() == 20L + RID_ENDS_WITH_X) { // 20X
			if (gTrip.getDirectionId() == 0) { // Village - NORTH
				if ("Village Exp".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Village Exp- Via Function Jct".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Cheakamus - SOUTH
				if ("Cheakamus Exp".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Cheakamus Exp- Via Function Jct".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (mRoute.getId() == 21L) {
			if (gTrip.getDirectionId() == 0) { // Village - NORTH
				if ("To Village".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Spring Creek - SOUTH
				if ("Spring Creek".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (mRoute.getId() == 25L) {
			if (gTrip.getDirectionId() == 0) { // Village - NORTH
				if ("To Village".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Whistler Creek - SOUTH
				if ("Whistler Creek".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (mRoute.getId() == 30L) {
			if (gTrip.getDirectionId() == 0) { // Alpine/Emerald - NORTH
				if ("Alpine/Emerald- Via Nesters".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Emerald- Via Nesters".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Village - SOUTH
				if ("To Village".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "To Village- Via Alpine".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (mRoute.getId() == 32L) {
			if (gTrip.getDirectionId() == 0) { // Emerald - NORTH
				if ("Emerald- Via Spruve Grv".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Village - SOUTH
				if ("To Village".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (mRoute.getId() == 99L) {
			if (gTrip.getDirectionId() == 0) { // Pemberton - NORTH
				if ("Commuter- To Pemberton".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Whistler - SOUTH
				if ("Commuter- to Whistler".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		}
		System.out.printf("\n%s: Unexpected trips headsign for %s!\n", mTrip.getRouteId(), gTrip);
		System.exit(-1);
		return;
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (MTrip.mergeEmpty(mTrip, mTripToMerge)) {
			return true;
		}
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					"Alpine", //
					"Emerald", //
					"Vlg" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Emerald", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Cheakamus", //
					"Spg Crk", //
					"Vlg", //
					"Whistler Crk" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Cheakamus", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					"Cheakamus", //
					"Function / Cheakamus" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Cheakamus", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					"Vlg", //
					"Vlg" + "-Via Function Jct", //
					"Via Function Jct" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Vlg", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Cheakamus", //
					"Cheakamus" + "- Via Function Jct", //
					"Via Function Jct" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Cheakamus", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20L + RID_ENDS_WITH_X) { // 20X
			if (Arrays.asList( //
					"Vlg Exp", //
					"Vlg Exp" + "- Via Function Jct", //
					"Via Function Jct" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Vlg Exp", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Cheakamus Exp", //
					"Cheakamus Exp" + "- Via Function Jct", //
					"Via Function Jct" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Cheakamus Exp", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 30L) {
			if (Arrays.asList( //
					"Alpine / Emerald", //
					"Alpine / Emerald" + "- Via Nesters", //
					"Emerald", //
					"Emerald" + "- Via Nesters" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Emerald", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Vlg", //
					"Vlg" + "- Via Alpine", //
					"Via Alpine" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Vlg", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s & %s.\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\W){1}(exchange)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("([\\s]?[\\-]?[\\s]?via .*$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^(.+ )?to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern FREE_SHUTTLE = Pattern.compile("((^|\\W){1}(free shuttle)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern EXPRESS_ = Pattern.compile("((^|\\W){1}(express|exp)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_DASH = Pattern.compile("([\\s]*[\\-]+[\\s]*$)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = EXCHANGE.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = FREE_SHUTTLE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = EXPRESS_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_DASH.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.CLEAN_PARENTHESE1.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_PARENTHESE1_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_PARENTHESE2.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_PARENTHESE2_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(\\-IMPL\\-\\)))", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_BOUND = Pattern.compile("(^(east|west|north|south)bound)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AT = Pattern.compile("((^|\\W){1}(at)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AT_REPLACEMENT = "$2/$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = STARTS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = AT.matcher(gStopName).replaceAll(AT_REPLACEMENT);
		gStopName = EXCHANGE.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
	}
}
