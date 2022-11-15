import json
from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, DEAD_DISPATCHER
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_3

from webob import Response
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.app.wsgi import ControllerBase, WSGIApplication, route

INSTANCE_NAME = 'task1_api'
URL = '/task1/{objective}'

class Task1Controller(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_3.OFP_VERSION]
    _CONTEXTS = {'wsgi': WSGIApplication}

    def __init__(self, *args, **kwargs):
        super(Task1Controller, self).__init__(*args, **kwargs)
        
        self.datapaths = {}
        wsgi = kwargs['wsgi']
        wsgi.register(Task1SwitchController, {INSTANCE_NAME: self})

    def add_flow(self, datapath, match, actions, priority, hard_timeout=0):
        ofp = datapath.ofproto
        ofp_parser = datapath.ofproto_parser

        inst = [ofp_parser.OFPInstructionActions(ofp.OFPIT_APPLY_ACTIONS, actions)]
        mod = ofp_parser.OFPFlowMod(datapath=datapath, priority=priority,
                                    hard_timeout=hard_timeout,
                                    match=match, instructions=inst)
        datapath.send_msg(mod)

    def clear_flows(self, datapath):
        ofp = datapath.ofproto
        ofp_parser = datapath.ofproto_parser

        clear_msg = ofp_parser.OFPFlowMod(
                        datapath=datapath,
                        table_id=ofp.OFPTT_ALL,
                        command=ofp.OFPFC_DELETE,
                        out_port=ofp.OFPP_ANY,
                        out_group=ofp.OFPG_ANY)
        datapath.send_msg(clear_msg)

    @set_ev_cls(ofp_event.EventOFPStateChange, [MAIN_DISPATCHER, DEAD_DISPATCHER])
    def on_state_change(self, ev):
        datapath = ev.datapath
        if ev.state == MAIN_DISPATCHER:
            if not datapath.id in self.datapaths:
                self.logger.info('Register datapath: %016x', datapath.id)
                self.datapaths[datapath.id] = datapath
        elif ev.state == DEAD_DISPATCHER:
            if datapath.id in self.datapaths:
                self.logger.info('Unregister datapath: %016x', datapath.id)
                del self.datapaths[datapath.id]

    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def on_switch_features(self, ev):
        datapath = ev.msg.datapath
        self._install_table_miss(datapath)
        self.logger.info('Switch: %s Connected', datapath.id)

    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def on_packet_in(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        in_port = msg.match['in_port']
        dpid = datapath.id

        pkt = packet.Packet(msg.data)
        eth_pkt = pkt.get_protocols(ethernet.ethernet)[0]
        dst_mac = eth_pkt.dst

        self.logger.info('Received a packet!')
    
    def _install_table_miss(self, datapath):
        ofp = datapath.ofproto
        ofp_parser = datapath.ofproto_parser

        match = ofp_parser.OFPMatch()
        actions = [ofp_parser.OFPActionOutput(ofp.OFPP_CONTROLLER, ofp.OFPCML_NO_BUFFER)]
        self.add_flow(datapath, match=match, actions=actions, priority=0)

    def _clear_all(self):
        for dpid, dp in self.datapaths.items():
            self.clear_flows(dp)

    def _install_table_miss_all(self):
        for dpid, dp in self.datapaths.items():
            self._install_table_miss(dp)

    def _set_dp1_normal(self):
        datapath = self.datapaths[1]
        ofp_parser = datapath.ofproto_parser
        
        match = ofp_parser.OFPMatch(in_port=1, eth_dst='00:00:00:00:00:02')
        actions = [ofp_parser.OFPActionOutput(2)]
        self.add_flow(datapath, match=match, actions=actions, priority=1)
        
        match = ofp_parser.OFPMatch(in_port=2, eth_dst='00:00:00:00:00:01')
        actions = [ofp_parser.OFPActionOutput(1)]
        self.add_flow(datapath, match=match, actions=actions, priority=1)

    def _set_dp2_normal(self):
        datapath = self.datapaths[2]
        ofp_parser = datapath.ofproto_parser

        match = ofp_parser.OFPMatch(in_port=1, eth_dst='00:00:00:00:00:01')
        actions = [ofp_parser.OFPActionOutput(2)]
        self.add_flow(datapath, match=match, actions=actions, priority=1)
        
        match = ofp_parser.OFPMatch(in_port=2, eth_dst='00:00:00:00:00:02')
        actions = [ofp_parser.OFPActionOutput(1)]
        self.add_flow(datapath, match=match, actions=actions, priority=1)

    def _set_dp3_normal(self):
        pass

    def _set_dp4_normal(self):
        pass

    def _set_dp1_slow(self):
        # TODO: complete if needed
        pass

    def _set_dp2_slow(self):
        # TODO: complete if needed
        pass

    def _set_dp3_slow(self):
        # TODO: complete if needed
        pass

    def _set_dp4_slow(self):
        # TODO: complete if needed
        pass

    def _set_dp1_veryslow(self):
        # TODO: complete if needed
        pass

    def _set_dp2_veryslow(self):
        # TODO: complete if needed
        pass

    def _set_dp3_veryslow(self):
        # TODO: complete if needed
        pass

    def _set_dp4_veryslow(self):
        # TODO: complete if needed
        pass

    def clear_all(self):
        self._clear_all()
        self._install_table_miss_all()
        return {'status': 'clear'}

    def set_normal(self):
        self._clear_all()
        self._install_table_miss_all()
        self._set_dp1_normal()
        self._set_dp2_normal()
        self._set_dp3_normal()
        self._set_dp4_normal()
        return {'status': 'normal'}

    def set_slow(self):
        self._clear_all()
        self._install_table_miss_all()
        self._set_dp1_slow()
        self._set_dp2_slow()
        self._set_dp3_slow()
        self._set_dp4_slow()
        return {'status': 'slow'}

    def set_veryslow(self):
        self._clear_all()
        self._install_table_miss_all()
        self._set_dp1_veryslow()
        self._set_dp2_veryslow()
        self._set_dp3_veryslow()
        self._set_dp4_veryslow()
        return {'status': 'veryslow'}


class Task1SwitchController(ControllerBase):
    def __init__(self, req, link, data, **config):
        super(Task1SwitchController, self).__init__(req, link, data, **config)
        self.task1_switch_app = data[INSTANCE_NAME]

    @route('task1', URL, methods=['GET'])
    def set_objective(self, req, **kwargs):
        task1_switch = self.task1_switch_app
        objective = kwargs['objective']

        if objective not in ['clear', 'normal', 'slow', 'veryslow']:
            return Response(status=404)

        if objective == 'clear':
            output = task1_switch.clear_all()
        elif objective == 'normal':
            output = task1_switch.set_normal()
        elif objective == 'slow':
            output = task1_switch.set_slow()
        else:
            output = task1_switch.set_veryslow()

        body = json.dumps(output)
        print(body)
        return Response(content_type='application/json', body=body.encode('utf-8'))