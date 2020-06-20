package org.mtransit.parser.ca_whistler_transit_system_bus;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.commons.StrategicMappingCommons;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
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
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		MTLog.log("Generating Whistler Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating Whistler Transit System bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
	public long getRouteId(GRoute gRoute) { // used by GTFS-RT
		return super.getRouteId(gRoute); // used by GTFS-RT
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
		String routeColor = gRoute.getRouteColor();
		if ("000000".equals(routeColor)) {
			routeColor = null; // ignore black
		}
		if (StringUtils.isEmpty(routeColor)) {
			if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				if ("20X".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return "004B8D";
				} else if ("25X".equalsIgnoreCase(gRoute.getRouteShortName())) {
					return "EC1A8D";
				}
			}
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 4: return "00A84F";
			case 5: return "8D0B3A";
			case 6: return "FFC10E";
			case 7: return "B2A97E";
			case 8: return "F399C0";
			case 10: return AGENCY_COLOR_BLUE; // TODO
			case 20: return "004B8D";
			case 21: return "F7921E";
			case 25: return "EC1A8D";
			case 30: return "00ADEE";
			case 31: return "A54499";
			case 32: return "8BC53F";
			case 99: return "5D86A0";
			// @formatter:on
			default:
				MTLog.logFatal("Unexpected route color %s!", gRoute);
				return null;
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		//noinspection UnnecessaryLocalVariable
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
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

	private final HashMap<Long, Long> routeIdToShortName = new HashMap<>();

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		final long rsn;
		if (!Utils.isDigitsOnly(mRoute.getShortName())) {
			Matcher matcher = DIGITS.matcher(mRoute.getShortName());
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
				String rsnString = mRoute.getShortName().toLowerCase(Locale.ENGLISH);
				if (rsnString.endsWith("w")) {
					rsn = digits + RID_ENDS_WITH_W;
				} else if (rsnString.endsWith("x")) {
					rsn = digits + RID_ENDS_WITH_X;
				} else {
					MTLog.logFatal("Unexpected route ID for %s!", mRoute);
					return;
				}
			} else {
				rsn = Long.parseLong(mRoute.getShortName());
			}
		} else {
			rsn = Long.parseLong(mRoute.getShortName());
		}
		this.routeIdToShortName.put(mRoute.getId(), rsn);
		if (rsn == 4L) {
			if (gTrip.getDirectionId() == 1) { // Marketplace - Free Shuttle - COUNTERCLOCKWISE
				if (Arrays.asList( //
						"Marketplace - Free Shuttle" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.COUNTERCLOCKWISE);
					return;
				}
			}
		} else if (rsn == 5L) {
			if (gTrip.getDirectionId() == 0) { // Upper Vlg - Benchlands - CLOCKWISE
				if ("Upper Vlg - Benchlands - Free Shuttle".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.CLOCKWISE);
					return;
				}
			}
		} else if (rsn == 6L) {
			if (gTrip.getDirectionId() == 1) { // Tapley''s-Blueberry - COUNTERCLOCKWISE
				if ("Tapley's-Blueberry".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.COUNTERCLOCKWISE);
					return;
				}
			}
		} else if (rsn == 7L) {
			if (gTrip.getDirectionId() == 0) { // Staff Housing - CLOCKWISE
				if ("Staff Housing".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.CLOCKWISE);
					return;
				}
			}
		} else if (rsn == 8L) {
			if (true) { // #IS_GOOD_ENOUGH
				if (gTrip.getDirectionId() == 1) { // ??? - CLOCKWISE
					if ("Lost Lake Shuttle - Free Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
						mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.CLOCKWISE);
						return;
					}
				}
			}
		} else if (rsn == 10L) {
			if (gTrip.getDirectionId() == 0) { // Emerald - NORTH
				if (Arrays.asList( //
						"Valley Express to Emerald", //
						"Emerald Via Function Jct-Valley Exp" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Cheakamus - SOUTH
				if (Arrays.asList( //
						"Valley Express to Cheakamus", //
						"Cheakamus Via Function Jct-Valley Exp" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (rsn == 20L) {
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
		} else if (rsn == 20L + RID_ENDS_WITH_X) { // 20X
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
		} else if (rsn == 21L) {
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
		} else if (rsn == 25L) {
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
		} else if (rsn == 30L) {
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
		} else if (rsn == 31L) {
			if (gTrip.getDirectionId() == 0) { // Alpine - NORTH
				if ("Alpine- Via Nesters".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Village - SOUTH
				if ("To village".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (rsn == 32L) {
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
		} else if (rsn == 99L) {
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
		MTLog.logFatal("%s:%s Unexpected trips head-sign for %s!", mTrip.getRouteId(), mRoute.getShortName(), gTrip.toStringPlus());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (MTrip.mergeEmpty(mTrip, mTripToMerge)) {
			return true;
		}
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		final long rsn = this.routeIdToShortName.get(mTrip.getRouteId());
		if (rsn == 1L) {
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
		} else if (rsn == 2L) {
			if (Arrays.asList( //
					"Cheakamus", //
					"Function / Cheakamus" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Cheakamus", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 20L) {
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
		} else if (rsn == 20L + RID_ENDS_WITH_X) { // 20X
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
		} else if (rsn == 30L) {
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
		MTLog.logFatal("Unexpected trips to merge %s & %s.", mTrip, mTripToMerge);
		return false;
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\W)(exchange)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("([\\-]?via .*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern FREE_SHUTTLE_SERVICE = Pattern.compile("((^|\\W)(free (service|shuttle))(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FREE_SHUTTLE_SERVICE_REPLACEMENT = "$2" + StringUtils.EMPTY + "$5";

	private static final Pattern ENDS_WITH_FREE_SERVICE = Pattern.compile("( + (free (service|shuttle))$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern EXPRESS_ = Pattern.compile("((^|\\W)(express|exp)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EXPRESS_REPLACEMENT = "$2" + StringUtils.EMPTY + "$4";

	private static final Pattern ENDS_WITH_DASH = Pattern.compile("([\\s]*[\\-]+[\\s]*$)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = EXCHANGE.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = FREE_SHUTTLE_SERVICE.matcher(tripHeadsign).replaceAll(FREE_SHUTTLE_SERVICE_REPLACEMENT);
		tripHeadsign = ENDS_WITH_FREE_SERVICE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = EXPRESS_.matcher(tripHeadsign).replaceAll(EXPRESS_REPLACEMENT);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = ENDS_WITH_DASH.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_PARENTHESE1.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_PARENTHESE1_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_PARENTHESE2.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_PARENTHESE2_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STARTS_WITH_DCOM = Pattern.compile("(^(\\(-DCOM-\\)))", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(-IMPL-\\)))", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_DCOM.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = EXCHANGE.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) { // used by GTFS-RT
		return super.getStopId(gStop);
	}
}
